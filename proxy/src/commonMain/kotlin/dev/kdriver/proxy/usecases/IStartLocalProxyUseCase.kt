package dev.kdriver.proxy.usecases

import dev.kaccelero.usecases.IPairUseCase
import dev.kdriver.proxy.Proxy

interface IStartLocalProxyUseCase : IPairUseCase<Int, Proxy, Unit>
