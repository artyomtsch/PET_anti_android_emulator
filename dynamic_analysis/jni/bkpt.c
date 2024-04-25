#include <stdlib.h> // avoid exit warning
#include <sys/wait.h> // waitpid()
#include <unistd.h> // fork(), sleep()
#include <signal.h>
#include <jni.h>

/*
  Большинство физ. устройств в ответ на инструкцию BKPT сигнализирут SIGTRAP || SIGBUS
  Хендлеры обрабатывают сигнал, завершают работу кода и возвращают -1 (физ. устройство)
*/

void handler_sigtrap(int signo) {
  exit(-1);
}

void handler_sigbus(int signo) {
  exit(-1);
}


void setupSigTrap() {
  signal(SIGTRAP, handler_sigtrap);
  signal(SIGBUS, handler_sigbus);
}

// QEMU в ответ на BKPT останавливает работу, как в режиме дебаггера
void tryBKPT() {  
  #if defined(arm)
    asm volatile ("bkpt 255");
  #endif
}

jint Java_anti_emulator_DynamicHeuristics_qemuBkpt(JNIEnv* env, jobject jObject) {
  
  pid_t child = fork();
  int child_status, status = 0;

  // child
  if(child == 0) {
    setupSigTrap();
    tryBKPT();
  } 
  // fork fail
  else if(child == -1) {
    status = -1;
  } 
  // parent
  else {
    int timeout = 0;
    int i = 0;
    // Ждем завершения child
    while ( waitpid(child, &child_status, WNOHANG) == 0 ) {
      sleep(1);
      // Если child не завершился после 2 секунд, имеем дело с эмулятором
      if(i++ == 1) {
        timeout = 1;
        break;
      }
    }

    // вошли в дебаг и это эмулятор
    if(timeout == 1) {
      status = 1;
    }
    // WIFEXITED == true, если процесс завершился нормально (в хендлере), физ. устройство
    if ( WIFEXITED(child_status) ) {
      status = 0;
    // Если процесс завершился с ошибкой, то эмулятор
    } else {
      status = 2;
    }

    kill(child, SIGKILL);
  }

  return status;
}
