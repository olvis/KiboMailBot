/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package bo.com.kibo.mailbot.impl;

import bo.com.kibo.mailbot.intf.IInterpretadorMensaje;
import java.util.HashMap;
import java.util.Map;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

/**
 *
 * @author Olvinho
 */
public abstract class InterpretadorMensajeGenerico implements IInterpretadorMensaje{
    
    private static final ThreadLocal<Map<String, IInterpretadorMensaje>> caja = new ThreadLocal<>();
    
    public static Map<String, IInterpretadorMensaje> getMapaObjetos(){
        Map<String, IInterpretadorMensaje> mapa = caja.get();
        if (mapa == null){
            mapa = new HashMap<>();
            mapa.put("area", new InterpretadorMensajeArea());
            
            caja.set(mapa);
        }
        return mapa;
    }
    
    protected Integer idUsuario;
    protected String parametros;
    protected String nombreEntidad;

    public InterpretadorMensajeGenerico() {
        parametros = "";
    }
    
    
    @Override
    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    @Override
    public void setParametros(String parametros) {
        this.parametros = parametros;
    }


    @Override
    public Multipart interpretar() throws MessagingException{
        if (parametros == null || "".equals(parametros)){
            return null;
        }
        int i = parametros.indexOf(".");
        i = (i == -1) ? parametros.length() : i;
        String comando = parametros.substring(0, i);
        switch (comando) {
            case "plantilla":
                return enviarPlantilla();
            case "cargar":
                break;
        }
        return null;
    }
    
    private Multipart enviarPlantilla() throws MessagingException{
        Multipart cuerpo = new MimeMultipart();
        BodyPart mensaje = new MimeBodyPart();
        mensaje.setText("Te envio la plantilla de " + nombreEntidad);
        cuerpo.addBodyPart(mensaje);
        return cuerpo;
    }

    @Override
    public void setNombreEntidad(String nombre) {
        this.nombreEntidad = nombre;
    }
    
    
}
