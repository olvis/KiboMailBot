/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package bo.com.kibo.mailbot;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.Message;
import javax.mail.MessagingException;

/**
 *
 * @author Olvinho
 */
public class ProcesadorMensaje implements Runnable{

    private String emailFrom;
    private Message mensaje;

    public ProcesadorMensaje(String emailFrom, Message mensaje) {
        this.emailFrom = emailFrom;
        this.mensaje = mensaje;
    }
    
    @Override
    public void run() {
        try {
            System.out.println("De: " + mensaje.getFrom()[0].toString());
            System.out.println("Asunto: " + mensaje.getSubject());
        } catch (MessagingException ex) {
            Logger.getLogger(ProcesadorMensaje.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
