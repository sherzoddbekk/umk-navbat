package uz.jurabekov.guard.presentation.queue_management

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import uz.jurabekov.guard.core.network.ApiResult
import uz.jurabekov.guard.domain.model.QueueItem
import uz.jurabekov.guard.domain.model.QueueItemStatus
import uz.jurabekov.guard.domain.model.Roles
import uz.jurabekov.guard.domain.usecase.CallInfoLaneUseCase
import uz.jurabekov.guard.domain.usecase.GetPermitsUseCase
import uz.jurabekov.guard.domain.usecase.GetQueueByDateUseCase
import uz.jurabekov.guard.domain.usecase.MarkManualEntryUseCase

/**
 * Info-tablo (1/2/3-yo'lga chaqirish + "O'tkazildi") mantiqining testlari.
 *
 * Serverga ulanmaydi — [FakeQueueRepository] backend rolini o'ynaydi.
 * Ishga tushirish:  `gradlew testDebugUnitTest --tests "*InfoLane*"`
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QueueManagementInfoLaneTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    /* ============================================================
     * Yo'lga chaqirish
     * ============================================================ */

    @Test
    fun `yo'lga chaqirish - API chaqiriladi va item yangilanadi`() = runTest(dispatcher) {
        val repo = FakeQueueRepository(listOf(permittedItem()))
        val vm = createViewModel(repo)
        advanceUntilIdle()

        vm.onEvent(QueueManagementUiEvent.LaneCallClicked(permittedItem(), lane = 2))
        advanceUntilIdle()

        assertEquals(listOf(QUEUE_ID to 2), repo.laneCalls)
        assertEquals(2, vm.itemInState().infoLane)
        // So'rov tugagach tugmalar yana faollashadi.
        assertNull(vm.state.value.laneActionInProgressId)
    }

    @Test
    fun `chaqiruv xatosi - holat o'zgarmaydi, toast chiqadi`() = runTest(dispatcher) {
        val repo = FakeQueueRepository(listOf(permittedItem())).apply {
            callLaneResult = ApiResult.Error(message = "1-yo'l band")
        }
        val vm = createViewModel(repo)
        val effects = mutableListOf<QueueManagementUiEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.effect.toList(effects)
        }
        advanceUntilIdle()

        vm.onEvent(QueueManagementUiEvent.LaneCallClicked(permittedItem(), lane = 1))
        advanceUntilIdle()

        assertNull("Xatoda item o'zgarmasligi kerak", vm.itemInState().infoLane)
        assertEquals(
            listOf(QueueManagementUiEffect.ShowToast("1-yo'l band")),
            effects
        )
    }

    @Test
    fun `ikkita tez bosish - faqat bitta so'rov ketadi`() = runTest(dispatcher) {
        val repo = FakeQueueRepository(listOf(permittedItem())).apply { actionDelayMs = 500 }
        val vm = createViewModel(repo)
        advanceUntilIdle()

        vm.onEvent(QueueManagementUiEvent.LaneCallClicked(permittedItem(), lane = 1))
        vm.onEvent(QueueManagementUiEvent.LaneCallClicked(permittedItem(), lane = 3))
        advanceUntilIdle()

        assertEquals(listOf(QUEUE_ID to 1), repo.laneCalls)
    }

    /* ============================================================
     * "O'tkazildi"
     * ============================================================ */

    @Test
    fun `o'tkazildi - item manualPassed bo'ladi`() = runTest(dispatcher) {
        val repo = FakeQueueRepository(listOf(permittedItem(infoLane = 1)))
        val vm = createViewModel(repo)
        advanceUntilIdle()

        vm.onEvent(QueueManagementUiEvent.ManualPassClicked(permittedItem(infoLane = 1)))
        advanceUntilIdle()

        assertEquals(listOf(QUEUE_ID), repo.manualEntries)
        assertTrue(vm.itemInState().manualPassed)
    }

    /* ============================================================
     * Rol bo'yicha bo'lim ko'rinishi
     * ============================================================ */

    @Test
    fun `bo'lim matritsasi - har rol to'g'ri bo'limlarni ko'radi`() = runTest(dispatcher) {
        val cases = mapOf(
            Roles.ADMIN to listOf(P, G, B),
            Roles.CONTROLLER to listOf(P, G, B),
            Roles.OPERATOR to listOf(P, B),
            Roles.GUEST to listOf(P, B),
            Roles.GATE_INSPECTOR to listOf(G, B),
            "noma'lum_rol" to listOf(P, B)   // default: gate emas
        )
        cases.forEach { (role, expected) ->
            val vm = createViewModel(FakeQueueRepository(listOf(permittedItem())), role)
            advanceUntilIdle()
            assertEquals(
                "$role uchun bo'limlar",
                expected,
                vm.state.value.availableSections
            )
        }
    }

    @Test
    fun `darvoza_tekshiruv - default tanlangan bo'lim GATE bo'ladi`() = runTest(dispatcher) {
        // PERMIT_QUEUE (default) bu rolga ko'rinmaydi → birinchi mavjudiga o'tadi.
        val vm = createViewModel(FakeQueueRepository(emptyList()), Roles.GATE_INSPECTOR)
        advanceUntilIdle()
        assertEquals(QueueSection.GATE_QUEUE, vm.state.value.selectedSection)
    }

    @Test
    fun `gate roli emas - chaqiruv tugmasi API chaqirmaydi`() = runTest(dispatcher) {
        val repo = FakeQueueRepository(listOf(permittedItem()))
        val vm = createViewModel(repo, Roles.OPERATOR)
        advanceUntilIdle()

        assertEquals(false, vm.state.value.canManageInfoLane)

        // UI'da tugma ko'rinmaydi; VM darajasida ham himoya bor.
        vm.onEvent(QueueManagementUiEvent.LaneCallClicked(permittedItem(), lane = 1))
        advanceUntilIdle()

        assertTrue("Rol mos emas — API chaqirilmasligi kerak", repo.laneCalls.isEmpty())
    }

    /* ============================================================
     * Helpers
     * ============================================================ */

    private fun createViewModel(
        repo: FakeQueueRepository,
        role: String = Roles.ADMIN
    ) = QueueManagementViewModel(
        getQueueByDate = GetQueueByDateUseCase(repo),
        getPermits = GetPermitsUseCase(repo),
        callInfoLane = CallInfoLaneUseCase(repo),
        markManualEntry = MarkManualEntryUseCase(repo),
        repository = repo,
        authRepository = FakeAuthRepository(testUser(role))
    )

    /** Xom ro'yxatdan (OPEN) shu id'li item. */
    private fun QueueManagementViewModel.itemInState(): QueueItem =
        state.value.openItems.first { it.id == QUEUE_ID }

    private fun permittedItem(infoLane: Int? = null) = QueueItem(
        id = QUEUE_ID,
        uuid = "uuid-1",
        queueNumber = 3,
        plate = "80 X 226 BB",
        fullName = "Toyloq",
        hasPermit = true,
        status = QueueItemStatus.ENTERED,
        createdAtEpochMs = 0L,
        arrivedAtHHmm = "07:31",
        infoLane = infoLane
    )

    private companion object {
        const val QUEUE_ID = 13299L

        // Bo'lim qisqartmalari — matritsa testini o'qishli qilish uchun.
        val P = QueueSection.PERMIT_QUEUE
        val G = QueueSection.GATE_QUEUE
        val B = QueueSection.GIVEN_PERMITS
    }
}
