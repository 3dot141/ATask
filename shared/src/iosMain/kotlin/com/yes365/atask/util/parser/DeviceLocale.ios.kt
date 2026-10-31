package com.yes365.atask.util.parser

import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.NSLocale

internal actual fun isDayFirstLocale(tag: String): Boolean {
    val formatter = NSDateFormatter().apply {
        locale = NSLocale(localeIdentifier = tag.replace('-', '_'))
        dateStyle = NSDateFormatterShortStyle
        timeStyle = NSDateFormatterNoStyle
    }
    return isDayFirstPattern(formatter.dateFormat)
}
