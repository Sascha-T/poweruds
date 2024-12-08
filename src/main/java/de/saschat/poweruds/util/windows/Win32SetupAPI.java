package de.saschat.poweruds.util.windows;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.*;

public class Win32SetupAPI {
    private static final Arena arena = Arena.ofShared();
    private static final SymbolLookup lookup = SymbolLookup.libraryLookup("setupapi", arena);

    // HDEVINFO(GUID*, PCSTR, HWND, DWORD)
    public static final MemorySegment SetupDiGetClassDevsA$mem = lookup.find("SetupDiGetClassDevsA").get();
    public static final MethodHandle SetupDiGetClassDevsA = Linker.nativeLinker().downcallHandle(SetupDiGetClassDevsA$mem,
        FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, JAVA_LONG, JAVA_INT));


    // BOOL(HDEVINFO, DWORD, PSP_DEVINFO_DATA)
    public static final MemorySegment SetupDiEnumDeviceInfo$mem = lookup.find("SetupDiEnumDeviceInfo").get();
    public static final MethodHandle SetupDiEnumDeviceInfo = Linker.nativeLinker().downcallHandle(SetupDiEnumDeviceInfo$mem,
        FunctionDescriptor.of(JAVA_BOOLEAN, ADDRESS, ADDRESS, ADDRESS));


    // BOOL(HDEVINFO, PSP_DEVICE_INTERFACE_DATA, PSP_DEVICE_INTERFACE_DETAIL_DATA_A, DWORD, PDWORD, PSP_DEVINFO_DATA)
    public static final MemorySegment SetupDiGetDeviceInterfaceDetailA$mem = lookup.find("SetupDiGetDeviceInterfaceDetailA").get();
    public static final MethodHandle SetupDiGetDeviceInterfaceDetailA = Linker.nativeLinker().downcallHandle(SetupDiGetDeviceInterfaceDetailA$mem,
        FunctionDescriptor.of(JAVA_BOOLEAN, ADDRESS, ADDRESS, ADDRESS, JAVA_INT, ADDRESS, ADDRESS));


    // sizeof (double check)
    public static final long HDEVINFO$size = 8;
    public static final long SP_DEVINFO_DATA$size = 32;
    public static final long SP_DEVICE_INTERFACE_DATA$size = 32;
    public static final long SP_DEVICE_INTERFACE_DETAIL_DATA_A$size = 8;
}
