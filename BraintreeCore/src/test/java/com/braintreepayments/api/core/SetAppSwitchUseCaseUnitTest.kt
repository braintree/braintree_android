package com.braintreepayments.api.core

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.confirmVerified
import org.junit.Before
import org.junit.Test

class SetAppSwitchUseCaseUnitTest {

    private lateinit var appSwitchRepository: AppSwitchRepository
    private lateinit var deviceInspector: DeviceInspector
    private lateinit var setAppSwitchUseCase: SetAppSwitchUseCase

    @Before
    fun setUp() {
        appSwitchRepository = mockk(relaxed = true)
        deviceInspector = mockk()
        setAppSwitchUseCase = SetAppSwitchUseCase(appSwitchRepository, deviceInspector)
    }

    @Test
    fun `invoke sets isAppSwitchFlow true when all conditions are true`() {
        every { deviceInspector.isPayPalInstalled() } returns true

        setAppSwitchUseCase.invoke(
            merchantEnabledAppSwitch = true,
            appSwitchFlowFromPayPalResponse = true
        )

        verify { appSwitchRepository.isAppSwitchFlow = true }
        confirmVerified(appSwitchRepository)
    }

    @Test
    fun `invoke sets isAppSwitchFlow false when merchantEnabledAppSwitch is false`() {
        every { deviceInspector.isPayPalInstalled() } returns true

        setAppSwitchUseCase.invoke(
            merchantEnabledAppSwitch = false,
            appSwitchFlowFromPayPalResponse = true
        )

        verify { appSwitchRepository.isAppSwitchFlow = false }
        confirmVerified(appSwitchRepository)
    }

    @Test
    fun `invoke sets isAppSwitchFlow false when PayPal is not installed`() {
        every { deviceInspector.isPayPalInstalled() } returns false

        setAppSwitchUseCase.invoke(
            merchantEnabledAppSwitch = true,
            appSwitchFlowFromPayPalResponse = true
        )

        verify { appSwitchRepository.isAppSwitchFlow = false }
        confirmVerified(appSwitchRepository)
    }

    @Test
    fun `invoke sets isAppSwitchFlow false when appSwitchFlowFromPayPalResponse is false`() {
        every { deviceInspector.isPayPalInstalled() } returns true

        setAppSwitchUseCase.invoke(
            merchantEnabledAppSwitch = true,
            appSwitchFlowFromPayPalResponse = false
        )

        verify { appSwitchRepository.isAppSwitchFlow = false }
        confirmVerified(appSwitchRepository)
    }
}
