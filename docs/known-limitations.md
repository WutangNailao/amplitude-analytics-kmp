# Known Limitations

## iOS custom plugin property loss

> **Warning:** Custom KMP plugins registered with `amplitude.add(plugin)` can cause event
> properties to be lost on iOS, even when the plugin only returns the event unchanged.
> Avoid registering KMP plugins on iOS when event property preservation is required.

### Scope

This document describes the current KMP adapter with Amplitude-Swift **v1.16.2**.

This limitation affects events processed by custom KMP plugins through the iOS
`PluginMapper.kt` adapter. It is not a general limitation of event tracking without
custom KMP plugins, and does not mean that the native SDK's built-in plugins pass
through this adapter. Android does not have this particular property-conversion
limitation.

The adapter does not copy these maps from the native iOS event into the KMP event:

- `eventProperties`
- `userProperties`
- `groups`
- `groupProperties`

Plugins therefore receive `null` for these maps instead of the original contents.
For example, `event.eventProperties?.put("channel", "app_store")` does nothing
because the map is `null`. Creating a new map in the plugin allows adding properties,
but does not recover the original properties.

### Why properties are lost

The current adapter performs this conversion:

```text
Native iOS event with properties
    -> new KMP event without the property maps
    -> custom plugin execution
    -> new native iOS event built from the plugin's result
```

The last step creates a replacement event; it does not merge changes into the
original native event. As a result, a plugin that simply returns its input can
still cause property loss:

```kotlin
override fun execute(event: BaseEvent): BaseEvent? {
    return event // Does not preserve the original native property maps on iOS.
}
```

For example, a `Purchase` event with `amount = 99` and `currency = "CNY"` can emerge
from the adapter with its event name intact but without either property. Missing
user and group properties can also affect user-property updates and group analysis.

### Native interoperability constraint

The official Amplitude-Swift SDK provides an Objective-C wrapper named
`AMPProperties`. Its public interface supports reading, writing, and removing a
value **by a known key**, but does not expose all keys, iteration, or a dictionary
snapshot. This has been verified in the currently used Amplitude-Swift v1.16.2.

Kotlin/Native cinterop generates bindings to that Objective-C interface. It cannot
automatically expose the underlying Swift dictionaries or discover unknown keys.
Consequently, the current adapter cannot obtain a complete property map through
this interface.

This is distinct from native Swift plugins: they can access the Swift dictionaries
directly. Native Objective-C plugins can also modify and return the original event
wrapper without reconstructing the event, preserving other properties. The loss
described here results from this KMP library's incomplete copy-and-rebuild adapter,
not from native event tracking inherently discarding properties.

Writing properties from KMP to iOS is possible using `AMPProperties.set(key, value)`.
Supporting that direction does not solve reading all existing properties when an
event enters a KMP plugin in the current integration.

### Recommendations

- Do not register custom KMP plugins on iOS when you need to preserve event, user,
  or group properties. Restrict registration to Android in platform-specific code
  if appropriate.
- For event enrichment on iOS, add properties before calling `track()` instead of
  relying on a KMP plugin. This avoids this adapter only if no other registered KMP
  plugin processes the event.
- If native plugin processing is necessary, implement it in the native Swift
  integration rather than routing it through this adapter. This requires separate
  integration work; this library does not currently provide that bridge.
- Test the actual event payloads when using custom plugins on iOS. An unchanged
  event name or a successful upload does not establish that its properties survived.

This limitation remains unresolved. A complete solution would require additional
native bridge capabilities or a redesign of the KMP plugin adapter; this document
does not introduce either change.

## iOS callback event properties

iOS event callbacks return a KMP event built from the actual native callback event,
including its event type and readable fields such as user ID, device ID, timestamp,
insert ID, and revenue fields. They do not return a placeholder event.

However, `eventProperties`, `userProperties`, `groups`, and `groupProperties` remain
`null` in the callback event. The same `AMPProperties` interface limitation described
above prevents reconstructing these maps. This affects per-event callbacks and the
global configuration callback, even when no custom KMP plugin is registered. The
callback conversion does not itself remove properties from the event sent to the server.

Use the event type or insert ID to identify the callback event. If the application
needs the originally supplied properties in a per-event callback, retain them in
the callback closure; they are the input properties, not a snapshot of the final
native event after SDK processing.

The native SDK declares the callback event as non-null. If an unexpected null event
arrives through interop, the KMP callback is not invoked rather than receiving a
fabricated event.

## References

- [Current KMP adapter](../kmp/src/iosMain/kotlin/com/amplitude/kmp/mappings/ios/PluginMapper.kt)
- [Amplitude-Swift v1.16.2 Objective-C property wrapper](https://github.com/amplitude/Amplitude-Swift/blob/v1.16.2/Sources/Amplitude/ObjC/ObjCProperties.swift)
- [Amplitude-Swift v1.16.2 Objective-C event wrapper](https://github.com/amplitude/Amplitude-Swift/blob/v1.16.2/Sources/Amplitude/ObjC/ObjCBaseEvent.swift)
