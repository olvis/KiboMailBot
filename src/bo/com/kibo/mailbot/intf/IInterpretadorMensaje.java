/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package bo.com.kibo.mailbot.intf;

import javax.mail.MessagingException;
import javax.mail.Multipart;

/**
 *
 * @author Olvinho
 */
public interface IInterpretadorMensaje {
    
    void setIdUsuario(Integer idUsuario);
    
    void setParametros(String parametro);
   
    void setNombreEntidad(String nombre);
    
    Multipart interpretar()throws MessagingException;
    
}
