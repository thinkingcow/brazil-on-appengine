/*Copyright 2010-2011 Google Inc.
 *
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License.
 *You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing, software
 *distributed under the License is distributed on an "AS IS" BASIS,
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *See the License for the specific language governing permissions and
 *limitations under the License.
 */

package sunlabs.brazil.appengine;

import sunlabs.brazil.template.RewriteContext;
import sunlabs.brazil.template.Template;

import java.util.Properties;
import java.util.StringTokenizer;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * AppEngine version of SMTPHandler using JavaMail.
 * Only the "singleton" tag is supported (for now).
 * @author suhler@google.com
 *
 */

public class SendMail extends Template {
  
  /**
   * &lt;sendmail to|cc|bcc="a,b,c ...."  from="  ", subject="  " body="  " /&gt;
   * Messages are sent as plain text, unless "subtype" is used to set an alternamte
   * text subtype.
   * @param hr
   */

  public void tag_sendmail(RewriteContext hr) {
    debug(hr);
    hr.killToken();
    
    String to = hr.get("to");
    if (to == null) {
      debug(hr, "to missing");
      return;
    }
    Properties props = new Properties();
    Session session = Session.getDefaultInstance(props, null);
    try {
        Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(hr.get("from")));
        
        StringTokenizer st = new StringTokenizer(to, ", \t");
        while(st.hasMoreTokens()) {
          String rcpt = st.nextToken();
          msg.addRecipient(Message.RecipientType.TO,
              new InternetAddress(rcpt));
        }
        
        st = new StringTokenizer(hr.get("cc", ""), ", \t");
        while(st.hasMoreTokens()) {
          String rcpt = st.nextToken();
          msg.addRecipient(Message.RecipientType.CC,
              new InternetAddress(rcpt));
        }
        
        st = new StringTokenizer(hr.get("bcc", ""), ", \t");
        while(st.hasMoreTokens()) {
          String rcpt = st.nextToken();
          msg.addRecipient(Message.RecipientType.BCC,
              new InternetAddress(rcpt));
        }

        String subject = hr.get("subject");
        if (subject != null) {
          msg.setSubject(subject);
        }
        String body = hr.get("body");
        // String charset=hr.get("charset", "UTF-8");
        String type = hr.get("subtype","plain");
        if (body != null) {
          // msg.setText(body); // this works for plain text
          //msg.setText(body, charset, type); // XXX not supported on app engine
          msg.setContent(body, "text/" + type);
        }
        Transport.send(msg);
    } catch (AddressException e) {
        e.printStackTrace();
        debug(hr, e.getMessage());
    } catch (MessagingException e) {
        e.printStackTrace();
        debug(hr, e.getMessage());
    }
  }
}