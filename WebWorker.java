/*
 John Rivers
 1/31/16
 Modified program:
 1) To serve html files. If the GET request refers to a correct filename
  then the content of the file is delivered.
 2) If the request provides an incorrect filename string, server responds
  with an error. This changes the response status and the html message body.
 3) Served html files outputs proper information for the tags
  <cs371date> and <3cs71server>
* 
* Web worker: an object of this class executes in its own new thread
* to receive and respond to a single HTTP request. After the constructor
* the object executes on its "run" method, and leaves when it is done.
*
* One WebWorker object is only responsible for one client connection. 
* This code uses Java threads to parallelize the handling of clients:
* each WebWorker runs in its own thread. This means that you can essentially
* just think about what is happening on one client at a time, ignoring 
* the fact that the entirety of the webserver execution might be handling
* other clients, too. 
*
* This WebWorker class (i.e., an object of this class) is where all the
* client interaction is done. The "run()" method is the beginning -- think
* of it as the "main()" for a client interaction. It does three things in
* a row, invoking three methods in this class: it reads the incoming HTTP
* request; it writes out an HTTP header to begin its response, and then it
* writes out some HTML content for the response content. HTTP requests and
* responses are just lines of text (in a very particular format). 
*
**/

import java.net.Socket;
import java.lang.Runnable;
import java.io.*;
import java.util.Date;
import java.text.DateFormat;
import java.util.TimeZone;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.util.Date;

public class WebWorker implements Runnable
{

private Socket socket;

/**
* Constructor: must have a valid open socket
**/
public WebWorker(Socket s)
{
   socket = s;
}

/**
* Worker thread starting point. Each worker handles just one HTTP 
* request and then returns, which destroys the thread. This method
* assumes that whoever created the worker created it with a valid
* open socket object.
**/
public void run()
{
   System.err.println("Handling connection...");
   try {
      InputStream is = socket.getInputStream();
      OutputStream os = socket.getOutputStream();
      String path = readHTTPRequest(is);
      
      //String fileName = path.substring(0, path.indexOf("."));
      String fileType = path.substring(path.indexOf(".")+1, path.length());
      
   //   System.out.println("PRINTING AS STRING: " + contentType);
   
      System.out.println("FILE TYPE IS " + fileType);

      if(fileType.equals("jpeg"))
         writeHTTPHeader(os,"image/jpeg", path);
         
      else if(fileType.equals("gif"))
         writeHTTPHeader(os,"image/gif", path);
         
      else if(fileType.equals("png"))
         writeHTTPHeader(os,"image/png", path);
      
      else if(fileType.equals("html"))
         writeHTTPHeader(os,"text/html", path);
        
      writeContent(os, path, fileType);
      os.flush();
      socket.close();
   } catch (Exception e) {
      System.err.println("Output error: "+e);
   }
   System.err.println("Done handling connection.");
   return;
}

/**
* Read the HTTP request header.
**/
private String readHTTPRequest(InputStream is)
{
   String line, holdPath = "";
   BufferedReader r = new BufferedReader(new InputStreamReader(is));
   while (true) {
      try {
        while (!r.ready()) Thread.sleep(1);
        line = r.readLine();
        System.err.println("Request line: ("+line+")");
        // must remove the first characters to examine potential filename
	    String holdGetLine = line.substring(0,3);
		// checks for GET line that contains potential filename
	    if(holdGetLine.equals("GET")){
               // isolates and prints the file path
		holdPath = line.substring(5);
		holdPath = holdPath.substring(0, holdPath.indexOf(" "));
		System.err.println("HOLD PATH IS: " + holdPath);
	   } // end if
         if (line.length()==0) break;
      } catch (Exception e) {
         System.err.println("Request error: "+e);
         break;
      }
   }
   // returns string with filename
   return holdPath;
}

/**
* Write the HTTP header lines to the client network connection.
* @param os is the OutputStream object to write to
* @param contentType is the string MIME content type (e.g. "text/html")
**/
private void writeHTTPHeader(OutputStream os, String contentType, String path) throws Exception
{
   Date d = new Date();
   DateFormat df = DateFormat.getDateTimeInstance();
   df.setTimeZone(TimeZone.getTimeZone("GMT"));
   // need to check if a correct filename has been entered
   File f = new File(path);
   if(f.exists() && !f.isDirectory()){
      os.write("HTTP/1.1 200 OK\n".getBytes());
      os.write("Date: ".getBytes());
      os.write((df.format(d)).getBytes());
      os.write("\n".getBytes());
      os.write("Server: John's very own server\n".getBytes());
      //os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes());
      //os.write("Content-Length: 438\n".getBytes()); 
      os.write("Connection: close\n".getBytes());
      os.write("Content-Type: ".getBytes());
      os.write(contentType.getBytes());
      os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
   } // end if

   // changes status to 404 if file not found
   else{
      os.write("HTTP/1.1 404 Not Found\n".getBytes());
      os.write("Date: ".getBytes());
      os.write((df.format(d)).getBytes());
      os.write("\n".getBytes());
      os.write("Server: John's very own server\n".getBytes());
      //os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes());
      //os.write("Content-Length: 438\n".getBytes()); 
      os.write("Connection: close\n".getBytes());
      os.write("Content-Type: ".getBytes());
      os.write(contentType.getBytes());
      os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
   } // end else
   return;
}

/**
* Write the data content to the client network connection. This MUST
* be done after the HTTP header has been written out.
* @param os is the OutputStream object to write to
**/
private void writeContent(OutputStream os, String path, String fileType) throws Exception
{
    // try catch if correct filename is not entered
    try{
    // used to read path string
    BufferedReader br = new BufferedReader(new FileReader(path));
    String line = null;
    
    if(fileType.equals("jpeg") || fileType.equals("gif") || fileType.equals("png")){
    
       File file = new File(path);
       FileInputStream is = new FileInputStream(file);
       byte [] data = new byte[(int) file.length()];
       is.read(data);
       is.close();
       
       DataOutputStream dataOS = new DataOutputStream(os);
       dataOS.write(data);
       dataOS.close();
        
    } // end if
      
      // not a graphic file
      if(fileType.equals("html")){
        System.out.println("I GOT THIS FAR!");
        // need Date object for outputing information for date tags
        Date d = new Date();
        DateFormat df = DateFormat.getDateTimeInstance();
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        // reads until end of line string
        while((line = br.readLine()) != null){
		 // checks if date tag exists in file. outputs date if exists.
          if(line.equals("<cs371date>")){
             os.write("<br> Time: ".getBytes());
             os.write((df.format(d)).getBytes());
             os.write("<br>".getBytes());
          } // end if
          // checks if server tag exists in file. outputs server name if exists.
          if(line.equals("<cs371server>")){
             os.write("<br>".getBytes());
             os.write("Server: John's very own server\n".getBytes());
             os.write("<br>".getBytes());
          } // end if
          // if html file contains an image
         // String lineStart = line.substring(0, 3);
          if(line.length() >= 3 && (line.substring(0,3)).equals("<img")){
             os.write("image here!".getBytes());
          }
          // writes html file to browser
          os.write(line.getBytes());
        } // end while
      } // end if

   } // end try
  
   // outputs HTTP 404 error to browser if file doesn't exist
   catch(FileNotFoundException exception)
   {
        os.write("HTTP 404 Not Found\n".getBytes());
   }
}

} // end class