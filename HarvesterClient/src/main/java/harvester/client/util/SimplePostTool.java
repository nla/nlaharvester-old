package harvester.client.util;

/***
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.*;
import java.util.*;
import java.net.*;
import org.apache.log4j.Logger;


/***
 * A simple utility class for posting raw updates to a Solr server, 
 * has a main method so it can be run on the command line.
 * 
 */
public class SimplePostTool {
	
  private static Logger logger = Logger.getLogger(SimplePostTool.class);	
	
  public static final String DEFAULT_POST_URL = "http://localhost:8080/solr/update";
  public static final String POST_ENCODING = "UTF-8";
  public static final String VERSION_OF_THIS_TOOL = "1.2";
  
  private static final String DATA_MODE_FILES = "files";
  private static final String DATA_MODE_ARGS = "args";
  private static final String DATA_MODE_STDIN = "stdin";

  private static final Set<String> DATA_MODES = new HashSet<String>();
  static {
    DATA_MODES.add(DATA_MODE_FILES);
    DATA_MODES.add(DATA_MODE_ARGS);
    DATA_MODES.add(DATA_MODE_STDIN);
  }
  
  protected URL solrUrl;

  private class PostException extends RuntimeException {
    PostException(String reason,Throwable cause) {
      super(reason + " (POST URL=" + solrUrl + ")",cause);
    }
  }
  
 
  
  /*** Check what Solr replied to a POST, and complain if it's not what we expected.
   *  TODO: parse the response and check it XMLwise, here we just check it as an unparsed String  
   */
  static void warnIfNotExpectedResponse(String actual,String expected) {
    if(actual.indexOf(expected) < 0) {
      warn("Unexpected response from Solr: '" + actual + "' does not contain '" + expected + "'");
    }
  }
  
  static void warn(String msg) {
    logger.warn(msg);
  }

  static void info(String msg) {
    logger.info(msg);
  }

  static void fatal(String msg) throws IOException{
    logger.fatal(msg);
    throw new IOException(msg);
  }

  /***
   * Constructs an instance for posting data to the specified Solr URL 
   * (ie: "http://localhost:8983/solr/update")
   */
  public SimplePostTool(URL solrUrl) {
    this.solrUrl = solrUrl;
  }

  /***
   * Does a simple commit operation 
   */
  public void commit(Writer output) throws IOException {
    postData(new StringReader("<commit/>"), output);
  }


  /***
   * Reads data from the data reader and posts it to solr,
   * writes to the response to output
   */
  public void postData(Reader data, Writer output) throws IOException{

    HttpURLConnection urlc = null;
    try {
      urlc = (HttpURLConnection) solrUrl.openConnection();
      try {
        urlc.setRequestMethod("POST");
      } catch (ProtocolException e) {
        throw new PostException("Shouldn't happen: HttpURLConnection doesn't support POST??", e);
      }
      urlc.setDoOutput(true);
      urlc.setDoInput(true);
      urlc.setUseCaches(false);
      urlc.setAllowUserInteraction(false);
      urlc.setRequestProperty("Content-type", "text/xml; charset=" + POST_ENCODING);
      
      OutputStream out = urlc.getOutputStream();
      
      try {
        Writer writer = new OutputStreamWriter(out, POST_ENCODING);
        pipe(data, writer);
        writer.close();
      } catch (IOException e) {
        throw new PostException("IOException while posting data", e);
      } finally {
        if(out!=null) out.close();
      }
      
      InputStream in = urlc.getInputStream();
      try {
        Reader reader = new InputStreamReader(in);
        pipe(reader, output);
        reader.close();
      } catch (IOException e) {
        throw new PostException("IOException while reading response", e);
      } finally {
        if(in!=null) in.close();
      }
      
    } catch (IOException e) {
      try {
        fatal("Solr returned an error: " + urlc.getResponseMessage());
      } catch (IOException f) { }
      fatal("Connection error (is Solr running at " + solrUrl + " ?): " + e);
    } finally {
      if(urlc!=null) urlc.disconnect();
    }
  }

  /***
   * Pipes everything from the reader to the writer via a buffer
   */
  private static void pipe(Reader reader, Writer writer) throws IOException {
    char[] buf = new char[1024];
    int read = 0;
    while ( (read = reader.read(buf) ) >= 0) {
      writer.write(buf, 0, read);
    }
    writer.flush();
  }
}
