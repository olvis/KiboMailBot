/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bo.com.kibo.mailbot;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 *
 * @author Olvinho
 */
public class ProcesadorMensaje implements Runnable {

    private final Message mensaje;
    private final String emailFrom;

    public ProcesadorMensaje(String emailFrom, Message mensaje) {
        this.mensaje = mensaje;
        this.emailFrom = emailFrom;
        
    }

    @Override
    public void run() {
        try {
            StringBuilder s = new StringBuilder();
            s.append("----------------- Mensaje Numero ").append(mensaje.getMessageNumber()).append(" --------------------\n");
            s.append("De: ").append(InternetAddress.toString(mensaje.getFrom())).append("\n");
            s.append("Fecha de recibido: ").append(mensaje.getReceivedDate()).append("\n");
            s.append("Fecha de envio: ").append(mensaje.getSentDate()).append("\n");
            s.append("Asunto: ").append(mensaje.getSubject()).append("\n");
            s.append("Responde a: ").append(mensaje.getReplyTo()[0]).append("\n");
            s.append("Dirigido a: ").append(mensaje.getAllRecipients()[0]).append("\n");
            s.append("Content-Type: ").append(mensaje.getContentType()).append("\n");
            System.out.println(s.toString());

            MimeMessage respuesta = (MimeMessage) mensaje.reply(false);
            respuesta.setFrom(new InternetAddress(emailFrom));
            respuesta.setSubject("RE: " + mensaje.getSubject());
            respuesta.setReplyTo(mensaje.getAllRecipients());

            respuesta.addRecipient(Message.RecipientType.TO, mensaje.getReplyTo()[0]);
            // Create your new message part    
            // Create a multi-part to combine the parts    
            Multipart multipart = new MimeMultipart();
            
            
            BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText("Te respondo");

            
            multipart.addBodyPart(messageBodyPart);
            // Associate multi-part with message  
            respuesta.setContent(multipart);
            

            // Send message    
            Transport.send(respuesta);
            mensaje.setFlag(Flags.Flag.ANSWERED, true);
            System.out.println("Mensaje " +  mensaje.getMessageNumber() + " respondido");

        } catch (MessagingException ex) {
            Logger.getLogger(ProcesadorMensaje.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
