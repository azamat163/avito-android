package com.avito.instrumentation.suite.filter

import com.avito.instrumentation.configuration.InstrumentationFilter
import com.avito.instrumentation.configuration.InstrumentationFilter.Data.FromRunHistory.ReportFilter
import com.avito.instrumentation.configuration.InstrumentationFilter.FromRunHistory.RunStatus
import com.avito.instrumentation.createStub
import com.avito.instrumentation.report.ReadReport
import com.avito.instrumentation.suite.filter.TestsFilter.Signatures.Source
import com.avito.instrumentation.suite.filter.TestsFilter.Signatures.TestSignature
import com.avito.report.FakeReportsApi
import com.avito.report.ReportsFetchApi
import com.avito.report.model.ReportCoordinates
import com.avito.report.model.SimpleRunTest
import com.avito.report.model.Status
import com.avito.report.model.createStubInstance
import com.google.common.truth.Truth
import org.funktionale.tries.Try
import org.junit.Test
import java.io.File

internal class FilterFactoryImplTest {

    @Test
    fun `when filterData is empty then initial filters always contains ExcludedBySdk and ExcludeAnnotationFilter`() {
        val factory = creteFilterFactory(
            filter = InstrumentationFilter.Data.createStub(),
            coordinates = ReportCoordinates.createStubInstance()
        )

        val initialFilter = factory.createInitialFilter() as CompositionFilter

        Truth.assertThat(initialFilter.filters)
            .containsExactly(
                ExcludeBySdkFilter(),
                ExcludeAnnotationsFilter(setOf(FilterFactory.JUNIT_IGNORE_ANNOTATION))
            )
    }

    @Test
    fun `when filterData contains included annotations then initial filters have IncludeAnnotationFilter`() {
        val annotation = "Annotation"
        val factory = creteFilterFactory(
            filter = InstrumentationFilter.Data.createStub(
                annotations = Filter.Value(
                    included = setOf(annotation),
                    excluded = emptySet()
                )
            ),
            coordinates = ReportCoordinates.createStubInstance()
        )

        val initialFilter = factory.createInitialFilter() as CompositionFilter

        Truth.assertThat(initialFilter.filters)
            .containsAtLeastElementsIn(listOf(IncludeAnnotationsFilter(setOf(annotation))))
    }

    @Test
    fun `when filterData contains prefixes then initial filters have IncludeBySignatures, ExcludeBySignatures`() {
        val includedPrefix = "included_prefix"
        val excludedPrefix = "excluded_prefix"
        val factory = creteFilterFactory(
            filter = InstrumentationFilter.Data.createStub(
                prefixes = Filter.Value(
                    included = setOf(includedPrefix),
                    excluded = setOf(excludedPrefix)
                )
            ),
            coordinates = ReportCoordinates.createStubInstance()
        )

        val initialFilter = factory.createInitialFilter() as CompositionFilter

        Truth.assertThat(initialFilter.filters)
            .containsAtLeastElementsIn(
                listOf(
                    IncludeByTestSignaturesFilter(
                        source = Source.Code,
                        signatures = setOf(
                            TestSignature(
                                name = includedPrefix
                            )
                        )
                    ),
                    ExcludeByTestSignaturesFilter(
                        source = Source.Code,
                        signatures = setOf(
                            TestSignature(
                                name = excludedPrefix
                            )
                        )
                    )
                )
            )
    }

    @Test
    fun `when filterData includePrevious statuses and Report return list without that status then initial filters contain IncludeTestSignaturesFilters#Previous with empty signatures`() {
        val fakeReportsApi = FakeReportsApi()
        val coordinates = ReportCoordinates.createStubInstance()
        fakeReportsApi.enqueueTestsForRunId(coordinates, Try.Success(emptyList()))

        val factory = creteFilterFactory(
            filter = InstrumentationFilter.Data.createStub(
                previousStatuses = Filter.Value(
                    included = setOf(RunStatus.Failed),
                    excluded = emptySet()
                )
            ),
            reportsFetchApi = fakeReportsApi,
            coordinates = coordinates
        )

        val initialFilter = factory.createInitialFilter() as CompositionFilter

        val that = Truth.assertThat(initialFilter.filters)
        that.containsAtLeastElementsIn(
            listOf(
                IncludeByTestSignaturesFilter(
                    source = Source.PreviousRun,
                    signatures = emptySet()
                )
            )
        )
    }

    @Test
    fun `when filterData includePrevious statuses and Report return list then initial filters contain IncludeTestSignaturesFilters#Previous with included statuses`() {
        val fakeReportsApi = FakeReportsApi()
        val coordinates = ReportCoordinates.createStubInstance()
        fakeReportsApi.enqueueTestsForRunId(
            coordinates, Try.Success(
                listOf(
                    SimpleRunTest.createStubInstance(
                        name = "test1",
                        deviceName = "25",
                        status = Status.Success
                    ),
                    SimpleRunTest.createStubInstance(
                        name = "test2",
                        deviceName = "25",
                        status = Status.Lost
                    )
                )
            )
        )

        val factory = creteFilterFactory(
            filter = InstrumentationFilter.Data.createStub(
                previousStatuses = Filter.Value(
                    included = setOf(RunStatus.Success),
                    excluded = emptySet()
                )
            ),
            reportsFetchApi = fakeReportsApi,
            coordinates = coordinates
        )

        val initialFilter = factory.createInitialFilter() as CompositionFilter

        val that = Truth.assertThat(initialFilter.filters)
        that.containsAtLeastElementsIn(
            listOf(
                IncludeByTestSignaturesFilter(
                    source = Source.PreviousRun,
                    signatures = setOf(
                        TestSignature(
                            name = "test1",
                            deviceName = "25"
                        )
                    )
                )
            )
        )
    }

    @Test
    fun `when filterData excludePrevious statuses and Report return list then initial filters contain ExcludeTestSignaturesFilters#Previous with included statuses`() {
        val fakeReportsApi = FakeReportsApi()
        val coordinates = ReportCoordinates.createStubInstance()
        fakeReportsApi.enqueueTestsForRunId(
            coordinates, Try.Success(
                listOf(
                    SimpleRunTest.createStubInstance(
                        name = "test1",
                        deviceName = "25",
                        status = Status.Success
                    ),
                    SimpleRunTest.createStubInstance(
                        name = "test2",
                        deviceName = "25",
                        status = Status.Lost
                    )
                )
            )
        )

        val factory = creteFilterFactory(
            filter = InstrumentationFilter.Data.createStub(
                previousStatuses = Filter.Value(
                    included = emptySet(),
                    excluded = setOf(RunStatus.Success)
                )
            ),
            reportsFetchApi = fakeReportsApi,
            coordinates = coordinates
        )

        val initialFilter = factory.createInitialFilter() as CompositionFilter

        val that = Truth.assertThat(initialFilter.filters)
        that.containsAtLeastElementsIn(
            listOf(
                ExcludeByTestSignaturesFilter(
                    source = Source.PreviousRun,
                    signatures = setOf(
                        TestSignature(
                            name = "test1",
                            deviceName = "25"
                        )
                    )
                )
            )
        )
    }

    @Test
    fun `when filterData previousStatuses is empty then initial filters don't contain PreviousRun filters`() {
        val fakeReportsApi = FakeReportsApi()
        val coordinates = ReportCoordinates.createStubInstance()
        fakeReportsApi.enqueueTestsForRunId(
            coordinates, Try.Success(
                emptyList()
            )
        )

        val factory = creteFilterFactory(
            filter = InstrumentationFilter.Data.createStub(
                previousStatuses = Filter.Value(
                    included = emptySet(),
                    excluded = emptySet()
                )
            ),
            reportsFetchApi = fakeReportsApi,
            coordinates = coordinates
        )

        val initialFilter = factory.createInitialFilter() as CompositionFilter

        initialFilter.filters.forEach { filter ->
            val assert = Truth.assertThat(filter)
            assert
                .isNotInstanceOf(IncludeByTestSignaturesFilter::class.java)
            assert
                .isNotInstanceOf(ExcludeByTestSignaturesFilter::class.java)
        }
    }

    @Test
    fun `when filterData report is empty then initial filters don't contain Report filters`() {
        val fakeReportsApi = FakeReportsApi()
        val coordinates = ReportCoordinates.createStubInstance()
        fakeReportsApi.enqueueTestsForRunId(
            coordinates, Try.Success(
                emptyList()
            )
        )

        val factory = creteFilterFactory(
            filter = InstrumentationFilter.Data.createStub(),
            reportsFetchApi = fakeReportsApi,
            coordinates = coordinates
        )

        val initialFilter = factory.createInitialFilter() as CompositionFilter

        initialFilter.filters.forEach { filter ->
            val assert = Truth.assertThat(filter)
            assert
                .isNotInstanceOf(IncludeByTestSignaturesFilter::class.java)
            assert
                .isNotInstanceOf(ExcludeByTestSignaturesFilter::class.java)
        }
    }

    @Test
    fun `when filterData report is present and has includes then initial filters contain Report include filter`() {
        val reportId = "reportId"
        val fakeReportsApi = FakeReportsApi()
        fakeReportsApi.enqueueTestsForReportId(
            reportId, Try.Success(
                listOf(
                    SimpleRunTest.createStubInstance(
                        name = "test1",
                        deviceName = "25",
                        status = Status.Success
                    ),
                    SimpleRunTest.createStubInstance(
                        name = "test2",
                        deviceName = "25",
                        status = Status.Lost
                    )
                )
            )
        )

        val factory = creteFilterFactory(
            filter = InstrumentationFilter.Data.createStub(
                report = ReportFilter(
                    id = reportId,
                    statuses = Filter.Value(
                        included = setOf(RunStatus.Success),
                        excluded = emptySet()
                    )
                )
            ),
            reportsFetchApi = fakeReportsApi
        )

        val initialFilter = factory.createInitialFilter() as CompositionFilter

        Truth.assertThat(initialFilter.filters)
            .containsAtLeastElementsIn(
                listOf(
                    IncludeByTestSignaturesFilter(
                        source = Source.Report,
                        signatures = setOf(
                            TestSignature(
                                name = "test1",
                                deviceName = "25"
                            )
                        )
                    )
                )
            )
    }

    @Test
    fun `when filterData report is present and has excludes then initial filters contain Report exclude filter`() {
        val reportId = "reportId"
        val fakeReportsApi = FakeReportsApi()
        fakeReportsApi.enqueueTestsForReportId(
            reportId, Try.Success(
                listOf(
                    SimpleRunTest.createStubInstance(
                        name = "test1",
                        deviceName = "25",
                        status = Status.Success
                    ),
                    SimpleRunTest.createStubInstance(
                        name = "test2",
                        deviceName = "25",
                        status = Status.Lost
                    )
                )
            )
        )

        val factory = creteFilterFactory(
            filter = InstrumentationFilter.Data.createStub(
                report = ReportFilter(
                    id = reportId,
                    statuses = Filter.Value(
                        included = emptySet(),
                        excluded = setOf(RunStatus.Success)
                    )
                )
            ),
            reportsFetchApi = fakeReportsApi
        )

        val initialFilter = factory.createInitialFilter() as CompositionFilter

        Truth.assertThat(initialFilter.filters)
            .containsAtLeastElementsIn(
                listOf(
                    ExcludeByTestSignaturesFilter(
                        source = Source.Report,
                        signatures = setOf(
                            TestSignature(
                                name = "test1",
                                deviceName = "25"
                            )
                        )
                    )
                )
            )
    }

    @Test
    fun `when filterData report is present and statuses empty then initial filters don't contain Report filter`() {
        val reportId = "reportId"
        val fakeReportsApi = FakeReportsApi()
        fakeReportsApi.enqueueTestsForReportId(
            reportId, Try.Success(
                listOf(
                    SimpleRunTest.createStubInstance(
                        name = "test1",
                        deviceName = "25",
                        status = Status.Success
                    ),
                    SimpleRunTest.createStubInstance(
                        name = "test2",
                        deviceName = "25",
                        status = Status.Lost
                    )
                )
            )
        )

        val factory = creteFilterFactory(
            filter = InstrumentationFilter.Data.createStub(
                report = ReportFilter(
                    id = reportId,
                    statuses = Filter.Value(
                        included = emptySet(),
                        excluded = emptySet()
                    )
                )
            ),
            reportsFetchApi = fakeReportsApi
        )

        val initialFilter = factory.createInitialFilter() as CompositionFilter

        initialFilter.filters.forEach { filter ->
            val assert = Truth.assertThat(filter)
            assert
                .isNotInstanceOf(IncludeByTestSignaturesFilter::class.java)
            assert
                .isNotInstanceOf(ExcludeByTestSignaturesFilter::class.java)
        }
    }

    fun creteFilterFactory(
        filter: InstrumentationFilter.Data,
        impactAnalysisFile: File? = null,
        reportsFetchApi: ReportsFetchApi = FakeReportsApi(),
        coordinates: ReportCoordinates = ReportCoordinates.createStubInstance()
    ): FilterFactory {
        return FilterFactory.create(
            filterData = filter,
            impactAnalysisResult = impactAnalysisFile,
            reportCoordinates = coordinates,
            readReportFactory = ReadReport.Factory.create(reportsFetchApi)
        )
    }
}