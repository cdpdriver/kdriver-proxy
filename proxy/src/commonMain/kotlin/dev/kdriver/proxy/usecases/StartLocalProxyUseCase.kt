package dev.kdriver.proxy.usecases

import dev.kdriver.proxy.LocalProxyController
import dev.kdriver.proxy.Proxy

class StartLocalProxyUseCase : IStartLocalProxyUseCase {

    override fun invoke(input1: Int, input2: Proxy) =
        LocalProxyController.startProxy(input1, input2)

}
