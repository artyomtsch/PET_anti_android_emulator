public class DynamicHeuristics {
  // загрузка C-библиотеки  
  static {
       System.loadLibrary("bkpt");
  }

  // объявление native-функции из библиотеки
  private native int qemuBkpt();

  // проверка возможна только для архитектуры ARM
  public boolean checkForBreakpointReaction() {
    boolean isARM = false;
    
    for (String arch : android.os.Build.SUPPORTED_ABIS) {
      if (arch.equalsIgnoreCase("armeabi-v7a")) {
        isARM = true;
        break;
      }
    }

    if (isARM) {
      return qemuBkpt() > 0;
    }
    
    return false;
  }
}
