/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bo.com.kibo.mailbot;

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.util.MailConnectException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.AuthenticationFailedException;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.event.MessageCountAdapter;
import javax.mail.event.MessageCountEvent;
import javax.mail.search.FlagTerm;

/**
 *
 * @author Olvinho
 */
public class LectorBandejaCorreo implements Runnable {

    private boolean running;
    private ILectorBandejaEscuchador listener;

    private String usuario;
    private String contrasena;
    private String hostSMTP;
    private String hostIMAP;
    private int puertoSMTP;
    private int puertoIMAP;
    private boolean conexionSegura;
    private String dominio;
    private String email;

    private Properties propiedadesMail;
    Store store;
    Session sesion;
    IMAPFolder bandejaEntrada;

    private static final int NUMERO_HILOS = 10;
    private ExecutorService poolDeTareas;
    private Thread hiloPrincipal;
    private Thread hiloIntentoConexion;

    public LectorBandejaCorreo() {
        this.running = false;
    }

    public ILectorBandejaEscuchador getListener() {
        return listener;
    }

    public void setListener(ILectorBandejaEscuchador listener) {
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
        if (listener != null) {
            listener.alIniciar();
        }
        poolDeTareas = Executors.newFixedThreadPool(NUMERO_HILOS);

        while (isRunning()) {
            notificarEvento("Buscando nuevos mensajes");
            FlagTerm ft = new FlagTerm(new Flags(Flags.Flag.SEEN), false);
            try {
                Message[] mensajes = bandejaEntrada.search(ft);
                notificarEvento("Se han encontrado " + mensajes.length + " mensaje(s) nuevos");
                for (Message mensaje : mensajes) {
                    mensaje.setFlag(Flags.Flag.SEEN, true);
                    ProcesadorMensaje p = new ProcesadorMensaje(email, mensaje);
                    poolDeTareas.execute(p);
                }
                if (isRunning()) {
                    notificarEvento("Esperando nuevos mensajes");
                    bandejaEntrada.idle(true);
                    notificarEvento("Fin de la espera de nuevos mensajes");
                }
            } catch (MessagingException ex) {
                Logger.getLogger(LectorBandejaCorreo.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(LectorBandejaCorreo.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (listener != null) {
            listener.alParar();
        }
    }
    
    public void iniciar() {
        if (!isRunning()) {
            if ((hiloIntentoConexion != null) && (hiloIntentoConexion.isAlive())) {
                return;
            }
            leerPropiedades();
            prepararPropiedadesMail();
            crearHiloIntentoDeConexion();
        }
    }

    private void crearHiloIntentoDeConexion() {
        hiloIntentoConexion = new Thread(new Runnable() {
            @Override
            public void run() {
                String mensajeFalloEvento = "Fallo el intento de conexion";
                notificarEvento("Intentando conectarse.");
                try {
                    store = sesion.getStore();
                    if (!store.isConnected()) {
                        store.connect(hostIMAP, usuario, contrasena);
                    }
                    Folder f;
                    f = store.getFolder("INBOX");
                    if (!f.exists()) {
                        notificarError("No existe la carpeta bandeja de entrada");
                        notificarEvento(mensajeFalloEvento);
                        return;
                    }
                    if (!(f instanceof IMAPFolder)) {
                        notificarError("El servidor no soporta carpetas IMAP");
                        notificarEvento(mensajeFalloEvento);
                        return;
                    }
                    bandejaEntrada = (IMAPFolder) f;
                    bandejaEntrada.open(Folder.READ_WRITE);
                    bandejaEntrada.addMessageCountListener(new MessageCountAdapter() {
                        @Override
                        public void messagesAdded(MessageCountEvent e) {
                            super.messagesAdded(e);
                            notificarEvento("Se ha detectado la llegada de " + e.getMessages().length + " mensaje(s)");
                        }
                    });
                    notificarEvento("Se ha conectado al servidor exitosamente");
                    crearHiloPrincipal();
                } catch (NoSuchProviderException ex) {
                    notificarError("El servidor no soporta conecciones IMAP");
                    notificarEvento(mensajeFalloEvento);
                } catch (AuthenticationFailedException ex) {
                    notificarError("No se pudo autenticar con el servidor, revise usuario y contraseña");
                    notificarEvento(mensajeFalloEvento);
                } catch (MailConnectException ex) {
                    if (ex.getCause() instanceof UnknownHostException) {
                        notificarError("No se encuentra el servidor IMAP '" + hostIMAP + "', el servidor no existe no dispone de conexión de internet");
                    } else if (ex.getCause() instanceof ConnectException) {
                        notificarError("La conexión fue rechazada por el servidor '" + hostIMAP + "', el puerto especificado '" + puertoIMAP + "' al parecer no es el correcto");
                    } else {
                        notificarError("Error de conexión: " + ex.getMessage());
                    }
                    notificarEvento(mensajeFalloEvento);
                } catch (MessagingException ex) {
                    Logger.getLogger(LectorBandejaCorreo.class.getName()).log(Level.SEVERE, null, ex);
                    notificarError("Error inesperado al intentar conectarse: " + ex.getMessage());
                    notificarEvento(mensajeFalloEvento);
                }
            }
        });
        hiloIntentoConexion.start();
    }

    private void crearHiloPrincipal() {
        setRunning(true);
        hiloPrincipal = new Thread(this);
        hiloPrincipal.start();
    }

    private void prepararPropiedadesMail() {
        propiedadesMail = new Properties();
        String protocoloIMAP = (conexionSegura) ? "imaps" : "imap";
        propiedadesMail.setProperty("mail.store.protocol", protocoloIMAP);
        propiedadesMail.put("mail.smtp.host", hostSMTP);
        propiedadesMail.put("mail.smtp.socketFactory.port", puertoSMTP);
        if (conexionSegura) {
            propiedadesMail.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            propiedadesMail.put("mail.smtp.auth", "true");
        }
        propiedadesMail.put("mail.smtp.port", puertoSMTP);
        if (puertoIMAP != -1) {
            propiedadesMail.put("mail." + protocoloIMAP + ".port", puertoIMAP);
        }
        sesion = Session.getDefaultInstance(propiedadesMail,
                new javax.mail.Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(usuario, contrasena);
                    }
                });
    }

    private void leerPropiedades() {
        InputStream input = null;
        Properties prop = new Properties();
        try {
            input = new FileInputStream("config.properties");
            prop.load(input);
        } catch (FileNotFoundException | SecurityException ex) {
            Logger.getLogger(LectorBandejaCorreo.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException("No se encuentra el archivo de propiedes, o no tiene permisos para leer el archivo");
        } catch (IOException ex) {
            Logger.getLogger(LectorBandejaCorreo.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException("Error leyendo el archivo de propiedades: " + ex.getMessage());
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ex) {
                }
            }
        }
        if (prop.getProperty("usuario") == null) {
            throw new RuntimeException("No se encuentra la propiedad 'usuario' en el archivo de configuración");
        }
        if (prop.getProperty("contrasena") == null) {
            throw new RuntimeException("No se encuentra la propiedad 'contrasena' en el archivo de configuración");
        }
        if (prop.getProperty("host.smtp") == null) {
            throw new RuntimeException("No se encuentra la propiedad 'host.smtp' en el archivo de configuración");
        }
        if (prop.getProperty("host.imap") == null) {
            throw new RuntimeException("No se encuentra la propiedad 'host.imap' en el archivo de configuración");
        }
        if (prop.getProperty("puerto.imap") == null) {
            puertoIMAP = -1;
        } else {
            try {
                puertoIMAP = Integer.valueOf(prop.getProperty("puerto.imap"));
            } catch (NumberFormatException e) {
                throw new RuntimeException("La propiedad 'puero.imap' del archivo de configuración debe ser un número");
            }
        }

        if (prop.getProperty("puerto.smtp") == null) {
            throw new RuntimeException("No se encuentra la propiedad 'puerto.smtp' en el archivo de configuración");
        }
        if (prop.getProperty("conexion.segura") == null) {
            throw new RuntimeException("No se encuentra la propiedad 'conexion.segura' en el archivo de configuración");
        }
        if (prop.getProperty("dominio") == null) {
            throw new RuntimeException("No se encuentra la propiedad 'dominio' en el archivo de configuración");
        }

        usuario = prop.getProperty("usuario");
        contrasena = prop.getProperty("contrasena");
        hostSMTP = prop.getProperty("host.smtp");
        hostIMAP = prop.getProperty("host.imap");

        try {
            puertoSMTP = Integer.valueOf(prop.getProperty("puerto.smtp"));
        } catch (NumberFormatException e) {
            throw new RuntimeException("La propiedad 'puerto.smtp' del archivo de configuración debe ser un número");
        }

        conexionSegura = Boolean.valueOf(prop.getProperty("conexion.segura"));
        dominio = prop.getProperty("dominio");
        email = (usuario.contains(dominio)) ? usuario : usuario + "@" + dominio;
    }

    public void parar() {
        if (isRunning()) {
            setRunning(false);
            try {
                bandejaEntrada.getMessageCount();
            } catch (MessagingException ex) {
                Logger.getLogger(LectorBandejaCorreo.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void notificarEvento(String texto) {
        if (listener != null) {
            listener.alRecibirEvento(texto);
        }
    }

    private void notificarError(String mensaje) {
        if (listener != null) {
            listener.alOcurrirError(mensaje);
        }
    }

}
