package com.braintreepayments.api.core.usecase

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.braintreepayments.api.core.AppSwitchRepository
import com.braintreepayments.api.core.DeviceInspector
import com.braintreepayments.api.core.SetAppSwitchUseCase
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class SetAppSwitchUseCaseTest {

    private lateinit var context: Context
    private lateinit var appSwitchRepository: AppSwitchRepository
    private lateinit var sut: SetAppSwitchUseCase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        appSwitchRepository = AppSwitchRepository()
        val deviceInspector = DeviceInspector(context)
        sut = SetAppSwitchUseCase(appSwitchRepository, deviceInspector)
    }

    @Test(timeout = 1000)
    fun invoke_setsAppSwitchFlowToFalse_whenPayPalNotInstalled() {
        sut(merchantEnabledAppSwitch = true, appSwitchFlowFromPayPalResponse = true)
        assertFalse(appSwitchRepository.isAppSwitchFlow)
    }

    @Test(timeout = 1000)
    fun invoke_setsAppSwitchFlowToFalse_whenMerchantDisabledAppSwitch() {
        sut(merchantEnabledAppSwitch = false, appSwitchFlowFromPayPalResponse = true)
        assertFalse(appSwitchRepository.isAppSwitchFlow)
    }

    @Test(timeout = 1000)
    fun invoke_setsAppSwitchFlowToFalse_whenPayPalResponseDisablesAppSwitch() {
        sut(merchantEnabledAppSwitch = true, appSwitchFlowFromPayPalResponse = false)
        assertFalse(appSwitchRepository.isAppSwitchFlow)
    }
}
