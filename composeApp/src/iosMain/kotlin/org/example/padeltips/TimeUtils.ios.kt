package org.example.padeltips.UI

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

// Implementación real para iPhone
actual fun getCurrentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()