package util;

import util.LogLevel;

import java.text.SimpleDateFormat;

public class Logger {
//    static so we can access it w/o creating an obj of type Logger
//    final -> const
   private static final String RED = "\u001b[31m";
   private static final String GREEN = "\u001b[32m";
   private static final String YELLOW = "\u001b[33m";
   private static final String BLUE = "\u001b[34m";
   private static final String MAGENTA = "\u001b[35m";
   private static final String CYAN = "\u001b[36m";
   private static final String RESET = "\u001b[0m";

   private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

   // Default Case for Logger w/o LogLevel
   public static void log(String message) {
       log(message, LogLevel.Info);
   }

   public static void log(String message, LogLevel level) {
    // Goal: [2024-01-01 hh:mm:ss.SSS][thread name] Info: OurMessage
   String dateString = dateFormat.format(System.currentTimeMillis());
   String threadName = Thread.currentThread().getName();

   String finalString = "[" + dateString + "][" + threadName + "] " + level + ": ";
   switch (level){
       case Debug -> finalString = BLUE + finalString + RESET + message;
       case Info -> finalString = YELLOW + finalString + RESET + message;
       case Warn -> finalString = MAGENTA + finalString + RESET + message;
       case Error -> finalString = RED + finalString + RESET + message;
       case Success -> finalString = GREEN + finalString + RESET + message;
       case Status -> finalString = CYAN + finalString + RESET + message;
   }
       System.out.println(finalString);
   }
}

