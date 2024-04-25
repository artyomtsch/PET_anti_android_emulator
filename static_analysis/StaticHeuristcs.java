import android.content.Context;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.File;
import java.util.LinkedHashMap;

public class StaticHeuristics {
    private LinkedHashMap<String,String> deviceInfo = new LinkedHashMap();

    // Проверка на дефолтный номер телефона
    private boolean isEmulatorPhoneNumber(Context context) {
        String[] emulatorNumbers = {
                "15555215554", "15555215556", "15555215558", "15555215560",
                "15555215562", "15555215564", "15555215566", "15555215568",
                "15555215570", "15555215572", "15555215574", "15555215576",
                "15555215578", "15555215580", "15555215582", "15555215584"
        };

        boolean isEmulator = false;
        String phoneNumber = null;

        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        try {
            phoneNumber = telephonyManager.getLine1Number();
            for (String number : emulatorNumbers) {
                isEmulator = phoneNumber.contains(number);
                if (isEmulator)
                    break;
            }
        } catch (SecurityException e) {
            Log.d("isKnownPhoneNumber", "Permission denied");
        }

        deviceInfo.put("PhoneNumber", phoneNumber);
        return isEmulator;
    }

    // Дефолтный мобильный оператор
    private boolean isEmulatorMobileOperator(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        String operator = telephonyManager.getNetworkOperator().toLowerCase();
        deviceInfo.put("Operator", operator);
        return operator.equals("android");
    }

    // Дефолтный международный идентификатор абонента (IMSI)
    private boolean isEmulatorIMSI(Context context) {
        String[] knownIMSI = { "310260000000000" };

        boolean isEmulator = false;
        String imsi = null;

        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        try {
            imsi = telephonyManager.getSubscriberId();
            for (String known : knownIMSI) {
                isEmulator = imsi.equals(known);
                if (isEmulator)
                    break;
            }
        } catch (SecurityException e) {
            Log.d("isSpoofedIMSI", "Permission denied");
        }

        deviceInfo.put("IMSI", imsi);
        return isEmulator;
    }

    // Дефолтные значения android.os.Build (характеристики системы и железа)
    private boolean isEmulatorBuild() {
        // BOARD
        String board = Build.BOARD;
        deviceInfo.put("Build.BOARD", board);

        boolean isBOARD = board.contains("unknown");

        // BRAND
        String[] knownBrands = {
                "generic",
                "generic_x86",
                "TTVM"
        };

        boolean isBRAND = checkAndLog(knownBrands, "BRAND", Build.BRAND);

        // DEVICE
        String[] knownDevices = {
                "generic",
                "generic_x86",
                "Andy",
                "ttVM_Hdragon",
                "Droid4X",
                "nox",
                "generic_x86_64",
                "vbox86p"
        };

        boolean isDEVICE = checkAndLog(knownDevices, "DEVICE", Build.DEVICE);

        // HARDWARE
        String[] knownHardware = {
                "nox",
                "ttVM_x86",
                "ranchu",
                "goldfish",
                "vbox86"
        };

        boolean isHARDWARE = checkAndLog(knownHardware, "HARDWARE", Build.HARDWARE);

        // MODEL
        String[] knownModels = {
                "sdk",
                "google_sdk",
                "lator",
                "Droid4X",
                "TiantianVM",
                "Andy",
                "Android SDK built for x86_64",
                "Android SDK built for x86"
        };

        boolean isMODEL = checkAndLog(knownModels, "MODEL", Build.MODEL);

        // FINGERPRINT
        String[] knownFingerprints = {
                "generic/sdk/generic",
                "generic_x86/sdk_x86/generic_x86",
                "Andy",
                "ttVM_Hdragon",
                "generic_x86_64",
                "generic/google_sdk/generic",
                "vbox86p",
                "generic/vbox86p/vbox86p"
        };

        boolean isFINGERPRINT = checkAndLog(knownFingerprints, "FINGERPRINT", Build.FINGERPRINT);

        // PRODUCT
        String[] knownProducts = {
                "sdk",
                "Andy",
                "ttVM_Hdragon",
                "google_sdk",
                "Droid4X",
                "nox",
                "sdk_x86",
                "sdk_google",
                "vbox86p"
        };

        boolean isPRODUCT = checkAndLog(knownProducts, "PRODUCT", Build.PRODUCT);

        // MANUFACTURER
        String[] knownManufacturers = {
                "unknown",
                "Genymotion",
                "Andy",
                "MIT",
                "nox",
                "TiantianVM"
        };

        boolean isMANUFACTURER = checkAndLog(knownManufacturers, "MANUFACTURER", Build.MANUFACTURER);

        return isBOARD
                || isBRAND
                || isFINGERPRINT
                || isDEVICE
                || isMODEL
                || isHARDWARE
                || isPRODUCT
                || isMANUFACTURER;
    }


    private boolean checkAndLog(String[] knownValues, String buildProperty, String buildValue) {
        deviceInfo.put("Build." + buildProperty, buildValue);

        if (buildValue == null) {
            return false;
        }

        for (String knownValue : knownValues) {
            if (buildValue.contains(knownValue)) {
                deviceInfo.put("Build." + buildProperty, buildValue + " THIS!");
                return true;
            }
        }
        return false;
    }

    // Наличие директорий или файлов эмуляторов
    private boolean checkEmulatorFiles() {
        String[] GENY_FILES = {
                "/dev/socket/genyd",
                "/dev/socket/baseband_genyd"
        };
        String[] PIPES = {
                "/dev/socket/qemud",
                "/dev/qemu_pipe"
        };
        String[] X86_FILES = {
                "ueventd.android_x86.rc",
                "x86.prop",
                "ueventd.ttVM_x86.rc",
                "init.ttVM_x86.rc",
                "fstab.ttVM_x86",
                "fstab.vbox86",
                "init.vbox86.rc",
                "ueventd.vbox86.rc"
        };
        String[] ANDY_FILES = {
                "fstab.andy",
                "ueventd.andy.rc"
        };
        String[] NOX_FILES = {
                "fstab.nox",
                "init.nox.rc",
                "ueventd.nox.rc"
        };

        return isFileExists(GENY_FILES, "GENY_FILES")
                || isFileExists(PIPES, "QEMU_PIPES")
                || isFileExists(X86_FILES, "X86_FILES")
                || isFileExists(ANDY_FILES, "ANDY_FILES")
                || isFileExists(NOX_FILES, "NOX_FILES");
    }

    private boolean isFileExists(String[] files, String type) {
        boolean result = false;
        for (String fileName : files) {
            File file = new File(fileName);
            if (file.exists()) {
                result = true;
                deviceInfo.put(type, fileName + " THIS!");
            }
        }
        return result;
    }
}
