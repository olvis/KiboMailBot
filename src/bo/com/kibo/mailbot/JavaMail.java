/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bo.com.kibo.mailbot;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.search.FlagTerm;

/**
 *
 * @author Olvinho
 */
public class JavaMail implements Runnable {

    private boolean running;
    private IJavaMailListener listener;

    private String email;
    private String password;
    private String hostSMTP;
    private String hostIMAP;
    private int segundos;

    private Properties propiedadesMail;
    Store store;
    Session session;
    Folder bandejaEntrada;
    
    private ExecutorService poolDeHilos;
    private static final int NUMERO_HILOS = 10;

    public JavaMail() {
        this.running = false;
    }

    public IJavaMailListener getListener() {
        return listener;
    }

    public void setListener(IJavaMailListener listener) {
        this.listener = listener;
    }

    private synchronized boolean isRunning() {
        return running;
    }

    private synchronized void setRunning(boolean running) {
        this.running = running;
    }

    @Override
    public void run() {
        if (listener != null){
            listener.alIniciar();
        }
        poolDeHilos =  Executors.newFixedThreadPool(NUMERO_HILOS);
        while (isRunning()) {
            FlagTerm ft = new FlagTerm(new Flags(Flags.Flag.SEEN), false);
            try {
                notificarEvento("Buscando nuevos mensajes..");
                Message[] mensajes = bandejaEntrada.search(ft);
                notificarEvento("Se han encontrado " + mensajes.length + " mensaje(s) nuevos.");
                for(Message mensaje :mensajes){ 
                    mensaje.setFlag(Flags.Flag.SEEN, true);
                    ProcesadorMensaje procesadorMensaje = new ProcesadorMensaje(email, mensaje);
                    poolDeHilos.execute(procesadorMensaje);
                }
            } catch (MessagingException ex) {
                Logger.getLogger(JavaMail.class.getName()).log(Level.SEVERE, null, ex);
            }
            notificarEvento("Se volvera a buscar dentro de " + segundos + " segundo(s)");
            try {
                Thread.sleep(segundos*1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(JavaMail.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        try {
            if (bandejaEntrada.isOpen()) {
                bandejaEntrada.close(false);
            }
            if (store.isConnected()) {
                store.close();
            }
        } catch (MessagingException ex) {
            Logger.getLogger(JavaMail.class.getName()).log(Level.SEVERE, null, ex);
        }
        poolDeHilos.shutdown();
        if (listener != null){
            listener.alParar();
        }
    }

    public void iniciar() {
        if (!isRunning()) {
            //Leer archivo de configuracion
            leerPropiedades();
            pepararYConectar();
            setRunning(true);
            Thread t = new Thread(this);
            t.start();
        }
    }

    private void pepararYConectar() {
        propiedadesMail = new Properties();
        propiedadesMail.setProperty("mail.store.protocol", "imaps");
        propiedadesMail.put("mail.smtp.host", hostSMTP);
        propiedadesMail.put("mail.smtp.socketFactory.port", "465");
        propiedadesMail.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        propiedadesMail.put("mail.smtp.auth", "true");
        propiedadesMail.put("mail.smtp.port", "465");
        session = Session.getDefaultInstance(propiedadesMail,
                new javax.mail.Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(email, password);
                    }
                });
        try {
            store = session.getStore();
            store.connect(hostIMAP, email, password);
            bandejaEntrada = store.getFolder("INBOX");
            bandejaEntrada.open(Folder.READ_WRITE);
        } catch (NoSuchProviderException ex) {
            Logger.getLogger(JavaMail.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException("No se pudo conectar a la bandeja de entrada: " + ex.getMessage());
        } catch (MessagingException ex) {
            Logger.getLogger(JavaMail.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException("No se pudo conectar a la bandeja de entrada: " + ex.getMessage());
        }

    }

    private void leerPropiedades() {
        InputStream input = null;
        try {
            Properties prop = new Properties();
            input = new FileInputStream("config.properties");
            prop.load(input);
            email = prop.getProperty("email");
            password = prop.getProperty("password");
            hostSMTP = prop.getProperty("host.smtp");
            hostIMAP = prop.getProperty("host.mail");
            segundos = Integer.valueOf(prop.getProperty("intervalo", "3"));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(JavaMail.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException("No se encuentra el archivo de propiedes");
        } catch (IOException ex) {
            Logger.getLogger(JavaMail.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException("Error cargando el archivo de propiedades: " + ex.getMessage());
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ex) {
                    //Logger.getLogger(JavaMail.class.getName()).log(Level.SEVERE, null, ex);
                    System.out.println("Erro cerrando el archivo de propiedades: " + ex.getMessage());
                }
            }
        }
    }

    public void parar() {
        if (isRunning()) {
            setRunning(false);
        }
    }
    
    private void notificarEvento(String texto){
        if (listener != null){
            listener.alRecibirEvento(texto);
        }
    }

}
