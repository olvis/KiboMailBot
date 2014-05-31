/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package bo.com.kibo.mailbot.intf;

/**
 *
 * @author Olvinho
 */
public interface ILectorBandejaEscuchador {
    void alIniciar();
    void alParar();
    void alRecibirEvento(String mensaje);
    void alOcurrirError(String mensaje);
}
