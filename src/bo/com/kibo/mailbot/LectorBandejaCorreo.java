/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bo.com.kibo.mailbot;

import bo.com.kibo.bl.impl.control.FactoriaObjetosNegocio;
import bo.com.kibo.bl.intf.IUsuarioBO;
import bo.com.kibo.mailbot.impl.InterpretadorMensajeGenerico;
import bo.com.kibo.mailbot.impl.UtilitariosMensajes;
import bo.com.kibo.mailbot.intf.IInterpretadorMensaje;
import bo.com.kibo.mailbot.intf.ILectorBandejaEscuchador;
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.protocol.IMAPProtocol;
import com.sun.mail.util.MailConnectException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.mail.Address;
import javax.mail.AuthenticationFailedException;
import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.FolderClosedException;
import javax.mail.IllegalWriteException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.NoSuchProviderException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.event.MessageCountAdapter;
import javax.mail.event.MessageCountEvent;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.FlagTerm;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

/**
 *
 * @author Olvinho
 */
public class LectorBandejaCorreo implements Runnable {

    private static final Logger LOG = Logger.getLogger(LectorBandejaCorreo.class.getName());

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
    private final int segundos = 10;

    private Properties propiedadesMail;
    private Store store;
    private Session sesion;
    private IMAPFolder bandejaEntrada;
    private Message[] nuevosMensajes;

    private final IMAPFolder.ProtocolCommand comandoNOP;

    private Thread hiloPrincipal;
    private Thread hiloIntentoConexion;
    private Thread hiloManterConexionActiva;

    public LectorBandejaCorreo() {
        this.running = false;
        this.comandoNOP = new IMAPFolder.ProtocolCommand() {
            @Override
            public Object doCommand(IMAPProtocol imapp) throws ProtocolException {
                if (imapp != null) {
                    imapp.simpleCommand("NOOP", null);
                }
                return null;
            }
        };
        nuevosMensajes = null;
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
        crearHiloMantenerConexion();
        while (isRunning()) {
            FlagTerm ft = new FlagTerm(new Flags(Flags.Flag.SEEN), false);
            try {
                if (!bandejaEntrada.isOpen()) {
                    notificarEvento("Abriendo bandeja de entrada");
                    bandejaEntrada.open(Folder.READ_WRITE);
                    if (!hiloManterConexionActiva.isAlive()) {
                        crearHiloMantenerConexion();
                    }
                }
                notificarEvento("Buscando nuevos mensajes");
                Message[] mensajes;
                if (nuevosMensajes != null) {
                    mensajes = bandejaEntrada.search(ft, nuevosMensajes);
                    nuevosMensajes = null;
                } else {
                    mensajes = bandejaEntrada.search(ft);
                }
                notificarEvento("Se han encontrado " + mensajes.length + " mensaje(s) nuevos");
                int i;
                for (i = 0; i < mensajes.length; i++) {
                    Message mensaje = mensajes[i];
                    mensaje.setFlag(Flags.Flag.SEEN, true);
                    notificarEvento("..Procesando mensaje " + (i + 1) + " de " + mensajes.length);
                    procesarMensaje(mensaje);
                    notificarEvento("..Mensaje " + (i + 1) + " de " + mensajes.length + " procesado");
                }
                if (isRunning()) {
                    notificarEvento("Esperando nuevos mensajes");
                    bandejaEntrada.idle(true);
                    notificarEvento("Fin de la espera de nuevos mensajes");
                }
            } catch (IllegalWriteException ex) {
                notificarError("La bandeja no permite marcar los mensajes como no leídos, esto provocará una lectura infinita. El lector se detendrá");
                break;
            } catch (FolderClosedException ex) {
                notificarEvento("La bandeja fue cerrada inesperadamente, se esperará " + segundos + " segundo(s) y se volverá a intentarlo");
                try {
                    Thread.sleep(segundos * 1000);
                } catch (InterruptedException ex1) {
                    notificarError(ex.getMessage());
                    break;
                }
            } catch (MessagingException ex) {
                LOG.log(Level.SEVERE, "Excepcion en el hilo principal", ex);
            }
        }
        notificarEvento("El lector se está deteniendo");

        if (hiloManterConexionActiva != null && hiloManterConexionActiva.isAlive()) {
            hiloManterConexionActiva.interrupt();
        }
        
        try {
            if (bandejaEntrada.isOpen()) {
                bandejaEntrada.close(false);
            }
            if (store.isConnected()) {
                store.close();
            }
        } catch (MessagingException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }

        if (listener != null) {
            listener.alParar();
        }
    }

    private static final Pattern patronMail = Pattern.compile("[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+");
    private IInterpretadorMensaje interprete;
    private File adjunto;

    private void procesarMensaje(Message mensaje) {
        int numeroMensaje = mensaje.getMessageNumber();
        try {
            String emailRemitente = "";
            String asunto = mensaje.getSubject();
            String remitente = mensaje.getFrom()[0].toString();
            Matcher matcher = patronMail.matcher(remitente);
            if (matcher.find()) {
                emailRemitente = matcher.group();
            }
            if (!"".equals(emailRemitente)) {
                //Verificamos si esta registrado
                Address[] arrayFrom = {new InternetAddress(email)};
                MimeMessage respuesta;
                respuesta = (MimeMessage) mensaje.reply(false);
                respuesta.setFrom(arrayFrom[0]);
                respuesta.setReplyTo(arrayFrom);
                respuesta.addRecipient(Message.RecipientType.TO, new InternetAddress(emailRemitente));
                IUsuarioBO usuarioBO = FactoriaObjetosNegocio.getInstance().getIUsuarioBO();
                Integer idUsuario = usuarioBO.getIdUsuarioPorEmail(emailRemitente);
                if (idUsuario != null) {
                    Multipart cuerpo = procesarPorAsunto(asunto, idUsuario);
                    if (cuerpo == null) {
                        cuerpo = procesarPorAdjunto(mensaje, idUsuario);
                    }
                    respuesta.setContent(cuerpo);
                } else {
                    respuesta.setContent(getMensajeUsuarioNoRegistrado());
                }
                notificarEvento("....Enviando respuesta");
                Transport.send(respuesta);
                notificarEvento("....La respuesta fue enviada");
                //Eliminamos archivos temporales
                if (interprete != null) {
                    for (File archivo : interprete.obtenerArchivoTemporalesCreados()) {
                        archivo.delete();
                    }
                    interprete.obtenerArchivoTemporalesCreados().clear();
                }
                if (adjunto != null) {
                    adjunto.delete();
                }
            } else {
                notificarEvento("....Imposible determinar el remitente, el mensaje será ignorado");
            }
        } catch (MessagingException ex) {
            LOG.log(Level.SEVERE, "Error procesando mensaje #" + numeroMensaje, ex);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "Error IO procesando mensaje #" + numeroMensaje, ex);
            notificarError(ex.getMessage());
        }
    }

    private Multipart procesarPorAsunto(String asunto, Integer idUsuario) throws MessagingException, IOException {
        asunto = corregirAsunto(asunto);
        if ("".equals(asunto)) {
            return null;
        }
        int i = asunto.indexOf(UtilitariosMensajes.SEPERADOR_PARAMETROS);
        if (i == -1) {
            return null;
        }
        String nombreEntidad = asunto.substring(0, i);
        nombreEntidad = nombreEntidad.replace(" ", "");
        interprete = InterpretadorMensajeGenerico.getMapaObjetos().get(nombreEntidad);
        if (interprete == null) {
            return null;
        }
        asunto = asunto.substring(i + 1, asunto.length());
        interprete.setParametros(asunto);
        interprete.setNombreEntidad(nombreEntidad);
        interprete.setIdUsuario(idUsuario);
        Multipart cuerpo;
        cuerpo = interprete.interpretar();
        return cuerpo;
    }

    private Multipart procesarPorAdjunto(Message mensaje, Integer idUsuario) throws MessagingException {
        adjunto = null;
        try {
            //No se pudo procesar por asunto, leer el adjunto si tiene
            adjunto = UtilitariosMensajes.bajarPrimerAdjunto(mensaje);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
            return getMensajeUsuarioAyuda();
        }
        if (adjunto == null){
            return getMensajeUsuarioAyuda();
        }
        FileInputStream fis = null;
        try {
           
            Workbook libro;
            fis = new FileInputStream(adjunto);
            libro = WorkbookFactory.create(fis);
            Sheet hoja = libro.getSheetAt(0);
            Row fila = hoja.getRow(0);
            if (fila == null) {
                return getMensajeUsuarioAyuda();
            }
            Cell celda = fila.getCell(0);
            if (celda == null) {
                return getMensajeUsuarioAyuda();
            }
            String nombreEntidad = celda.getStringCellValue();
            interprete = InterpretadorMensajeGenerico.getMapaObjetos().get(nombreEntidad);
            if (interprete == null) {
                return getMensajeUsuarioAyuda();
            }
            interprete.setIdUsuario(idUsuario);
            interprete.setNombreEntidad(nombreEntidad);
            return interprete.interpretarHojaExcel(hoja);
        } catch (IOException | InvalidFormatException ex) {
            LOG.log(Level.SEVERE, "Error Leyendo adjunto", ex);
            return getMensajeUsuarioAyuda();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ex) {
                    Logger.getLogger(LectorBandejaCorreo.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private String corregirAsunto(String asunto) {
        if (asunto == null) {
            return "";
        }

        asunto = asunto.toLowerCase().replaceAll(" ", ""); //llevamos a minusculas
        if (asunto.length() > 3) {
            if (asunto.substring(0, 2).equals("re:")) {
                asunto = asunto.substring(3, asunto.length() - 1);
            }
        } else if (asunto.length() == 3 && asunto.equals("re:")) {
            asunto = "";
        }
        return asunto;
    }

    private Multipart getMensajeUsuarioNoRegistrado() throws MessagingException {
        Multipart multiPartes = new MimeMultipart();
        BodyPart parte = new MimeBodyPart();
        parte.setText("Lo siento no está registrado para poder usar este sistema");
        multiPartes.addBodyPart(parte);
        return multiPartes;
    }

    private Multipart getMensajeUsuarioAyuda() throws MessagingException {
        Multipart multiPartes = new MimeMultipart();
        BodyPart parte = new MimeBodyPart();
        parte.setText("El mensaje no fue reconocido, esta es la ayuda");
        multiPartes.addBodyPart(parte);
        return multiPartes;
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
                notificarEvento("Intentando conectarse");
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
                            nuevosMensajes = e.getMessages();
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
                    LOG.log(Level.SEVERE, null, ex);
                    notificarError("Error inesperado al intentar conectarse: " + ex.getMessage());
                    notificarEvento(mensajeFalloEvento);
                }
            }
        }, "HiloIntentoDeConexion");
        hiloIntentoConexion.start();
    }

    private static final long MAXIMO_TIEMPO_ESPERANDO = 300000; // 5 minutos

    private void crearHiloMantenerConexion() {
        hiloManterConexionActiva = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Thread.sleep(MAXIMO_TIEMPO_ESPERANDO);
                        notificarEvento("Tiempo de espera máximo agotado");
                        bandejaEntrada.doCommand(comandoNOP);
                    } catch (InterruptedException | FolderClosedException ex) {
                        return;
                    } catch (MessagingException ex) {
                        LOG.log(Level.SEVERE, Thread.currentThread().getName(), ex);
                    }
                }
            }
        }, "HiloMantenerConexionActiva");
        hiloManterConexionActiva.start();
    }

    private void crearHiloPrincipal() {
        setRunning(true);
        hiloPrincipal = new Thread(this, "HiloPrincipalLectorBandeja");
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
            LOG.log(Level.WARNING, null, ex);
            throw new RuntimeException("No se encuentra el archivo de propiedes, o no tiene permisos para leer el archivo");
        } catch (IOException ex) {
            LOG.log(Level.WARNING, null, ex);
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
                bandejaEntrada.doCommand(comandoNOP);
            } catch (MessagingException ex) {
                LOG.log(Level.WARNING, Thread.currentThread().getName(), ex);
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
