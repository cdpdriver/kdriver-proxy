# kdriver-proxy

[![License](https://img.shields.io/github/license/cdpdriver/kdriver-proxy)](LICENSE)
[![Maven Central Version](https://img.shields.io/maven-central/v/dev.kdriver/proxy)](https://klibs.io/project/cdpdriver/kdriver-proxy)
[![Issues](https://img.shields.io/github/issues/cdpdriver/kdriver-proxy)]()
[![Pull Requests](https://img.shields.io/github/issues-pr/cdpdriver/kdriver-proxy)]()
[![codecov](https://codecov.io/github/cdpdriver/kdriver-proxy/branch/main/graph/badge.svg?token=F7K641TYFZ)](https://codecov.io/github/cdpdriver/kdriver-proxy)
[![CodeFactor](https://www.codefactor.io/repository/github/cdpdriver/kdriver-proxy/badge)](https://www.codefactor.io/repository/github/cdpdriver/kdriver-proxy)
[![Open Source Helpers](https://www.codetriage.com/cdpdriver/kdriver-proxy/badges/users.svg)](https://www.codetriage.com/cdpdriver/kdriver-proxy)

A lightweight SOCKS5 proxy server written in pure Kotlin. It allows you to create local SOCKS5
proxy endpoints that forward traffic through a remote HTTPS proxy with optional authentication.

Ideal for Kotlin applications needing dynamic local proxies without relying on external binaries
like [Gost](https://github.com/go-gost/gost), especially on platforms like Windows.

It's perfect for use with [KDriver](https://github.com/cdpdriver/kdriver).

## 📦 Installation

Add the dependency to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("dev.kdriver:proxy:0.2.0")
}
```

## 🚀 Features

* Start a local SOCKS5 proxy on any port
* Forward all traffic through a remote HTTPS proxy (with optional auth)
* Run multiple proxies in parallel
* Fully coroutine-based (non-blocking)
* Pure Kotlin (JVM)

## 🧪 Usage

### Start a Proxy

```kotlin
val startUseCase = StartLocalProxyUseCase()
startUseCase(1080, Proxy(URI("https://your-remote-proxy.com:443"), "yourUsername", "yourPassword"))
```

Now you can connect to: `socks5://localhost:1080`
Your traffic will go through the remote proxy with authentication.

### Stop a Proxy

```kotlin
val stopUseCase = StopLocalProxyUseCase()
stopUseCase(1080)
```
