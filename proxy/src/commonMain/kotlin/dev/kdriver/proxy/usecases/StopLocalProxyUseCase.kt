package dev.kdriver.proxy.usecases

import dev.kdriver.proxy.LocalProxyController

class StopLocalProxyUseCase : IStopLocalProxyUseCase {

    override fun invoke(input: Int) = LocalProxyController.stopProxy(input)

}
