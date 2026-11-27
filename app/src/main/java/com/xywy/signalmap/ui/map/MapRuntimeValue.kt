package com.xywy.signalmap.ui.map

/** Map callback/cache bookkeeping is not UI state: replacing listeners must not trigger another update. */
internal class MapRuntimeValue<T>(var value: T)
