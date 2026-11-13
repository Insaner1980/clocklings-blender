# Navigation 3 -avaimet sarjallistetaan kotlinx.serializationilla (paluupino tallentuu prosessin kuollessa).
-keepclassmembers @kotlinx.serialization.Serializable class fi.clocklings.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class fi.clocklings.**$$serializer { *; }
