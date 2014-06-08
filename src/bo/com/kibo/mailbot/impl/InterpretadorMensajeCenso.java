/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bo.com.kibo.mailbot.impl;

import bo.com.kibo.bl.exceptions.BusinessExceptionMessage;
import bo.com.kibo.bl.impl.control.FactoriaObjetosNegocio;
import bo.com.kibo.bl.intf.IFormularioCensoBO;
import bo.com.kibo.entidades.Area;
import bo.com.kibo.entidades.Calidad;
import bo.com.kibo.entidades.DetalleCenso;
import bo.com.kibo.entidades.Especie;
import bo.com.kibo.entidades.Faja;
import bo.com.kibo.entidades.FormularioCenso;
import bo.com.kibo.mailbot.intf.IInterpretadorFormularioDasometrico;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;

/**
 *
 * @author Olvinho
 * @version 1.0
 */
public class InterpretadorMensajeCenso 
extends InterpretadorMensajeGenerico<FormularioCenso, Integer, IFormularioCensoBO> 
implements IInterpretadorFormularioDasometrico {

    private Area area;

    @Override
    public void setArea(Area area) {
        this.area = area;
    }

    @Override
    public Area getArea() {
        return area;
    }

    @Override
    FormularioCenso convertirHojaEnEntidad() {
        FormularioCenso entidad = new FormularioCenso();
        Cell celda;
        //Id
        celda = getCelda(3, 2);
        if (celda.getCellType() == Cell.CELL_TYPE_NUMERIC) {
            entidad.setId((int) celda.getNumericCellValue());
        }

        //Area
        entidad.setArea(new Area());
        celda = getCelda(5, 2);
        entidad.getArea().setCodigo(getValorCeldaCadena(celda));

        //Fecha
        celda = getCelda(3, 5);
        if (DateUtil.isCellDateFormatted(celda)) {
            try {
                entidad.setFecha(celda.getDateCellValue());
            } catch (Exception e) {
                appendException(new BusinessExceptionMessage("La fecha no tiene un formato válido"));
            }
        }
        //Faja
        celda = getCelda(5, 5);
        String cadenaBloqueFaja = getValorCeldaCadena(celda);
        if (!"".equalsIgnoreCase(cadenaBloqueFaja)) {
            String[] partes = cadenaBloqueFaja.split("-");
            entidad.setFaja(new Faja());
            entidad.getFaja().setBloque(partes[0]);
            if (partes.length > 1) {
                try {
                    entidad.getFaja().setNumero(Integer.parseInt(partes[1]));
                } catch (Exception e) {
                    appendException(new BusinessExceptionMessage("El número de la faja debe ser un número entero"));
                }
            }
        }
        //Horas
        celda = getCelda(3, 8);
        if (celda.getCellType() != Cell.CELL_TYPE_BLANK) {
            if (celda.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                entidad.setHoras((byte) celda.getNumericCellValue());
            } else {
                appendException(new BusinessExceptionMessage("Las horas deben ser un valor númerico"));
            }
        }

        //Detalle
        int i = 9;
        int fila = 0;
        do {
            celda = getCelda(i, 1);
            fila++;
            if (celda.getCellType() != Cell.CELL_TYPE_BLANK) {
                DetalleCenso detalle = new DetalleCenso();
                //Codigo
                Cell celdaAux = getCelda(i, 1);
                detalle.setCodigo(getValorCeldaCadena(celdaAux));
                //Especie
                celdaAux = getCelda(i, 2);
                detalle.setEspecie(new Especie());
                detalle.getEspecie().setNombre(getValorCeldaCadena(celdaAux));

                //Altura
                celdaAux = getCelda(i, 3);
                if (celdaAux.getCellType() != Cell.CELL_TYPE_BLANK) {
                    if (celdaAux.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                        detalle.setAltura((float) celdaAux.getNumericCellValue());
                    } else {
                        appendException(new BusinessExceptionMessage("La altura debe ser un valor numérico", "altura", fila));
                    }
                }
                //Dap
                celdaAux = getCelda(i, 4);
                if (celdaAux.getCellType() != Cell.CELL_TYPE_BLANK) {
                    if (celdaAux.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                        detalle.setDap((float) celdaAux.getNumericCellValue());
                    } else {
                        appendException(new BusinessExceptionMessage("El DAP debe ser un valor numérico", "dap", fila));
                    }
                }
                //Calidad
                celdaAux = getCelda(i, 5);
                detalle.setCalidad(new Calidad());
                detalle.getCalidad().setCodigo(getValorCeldaCadena(celdaAux));
                //Condicion
                celdaAux = getCelda(i, 6);
                detalle.setCondicion(getValorCeldaCadena(celdaAux));
                //PuntoGPS
                celdaAux = getCelda(i, 7);
                if (celdaAux.getCellType() != Cell.CELL_TYPE_BLANK) {
                    if (celdaAux.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                        detalle.setPunto((int) celdaAux.getNumericCellValue());
                    } else {
                        appendException(new BusinessExceptionMessage("El PuntoGPS debe ser un valor numérico", "dap", fila));
                    }
                }
                //X
                celdaAux = getCelda(i, 8);
                if (celdaAux.getCellType() != Cell.CELL_TYPE_BLANK) {
                    if (celdaAux.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                        detalle.setX((float) celdaAux.getNumericCellValue());
                    } else {
                        appendException(new BusinessExceptionMessage("La coordenada X debe ser un valor númerico decimal", "X", fila));
                    }
                }
                //Y
                celdaAux = getCelda(i, 9);
                if (celdaAux.getCellType() != Cell.CELL_TYPE_BLANK) {
                    if (celdaAux.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                        detalle.setY((float) celdaAux.getNumericCellValue());
                    } else {
                        appendException(new BusinessExceptionMessage("La coordenada Y debe ser un valor númerico decimal", "Y", fila));
                    }
                }
                //Obs
                celdaAux = getCelda(i, 10);
                detalle.setObservaciones(getValorCeldaCadena(celdaAux));

                entidad.getDetalle().add(detalle);
            }
            i++;
        } while (celda.getCellType() != Cell.CELL_TYPE_BLANK);
        return entidad;
    }

    @Override
    IFormularioCensoBO getObjetoNegocio() {
        return FactoriaObjetosNegocio.getInstance().getFormularioCensoBO();
    }

    @Override
    boolean esNuevo(FormularioCenso entidad) {
        return (entidad.getId() == null);
    }

    @Override
    String getId(FormularioCenso entidad) {
        return entidad.getId().toString();
    }

    @Override
    Integer convertirId(String cadena) throws Exception {
        return convertirIdAEntero(cadena);
    }

    @Override
    void mostrarLista(List<FormularioCenso> lista) {
        int i = 5;
        for (FormularioCenso f : lista) {
            setValorCelda(i, 1, f.getId());
            setValorCelda(i, 2, f.getFecha());
            setValorCelda(i, 3, FactoriaObjetosNegocio.getInstance().
                    getAreaBO().getCodigo(f.getArea().getId()));
            i++;
        }
    }

    @Override
    void mostrarEntidad(FormularioCenso entidad) {
        setValorCelda(3, 2, entidad.getId());
        setValorCelda(3, 5, entidad.getFecha());
        setValorCelda(3, 8, entidad.getHoras());

        setValorCelda(5, 2, FactoriaObjetosNegocio.getInstance().getAreaBO().
                getCodigo(entidad.getArea().getId()));
        if (entidad.getFaja() != null) {
            Faja faja = FactoriaObjetosNegocio.getInstance().getFajaBO().
                    recuperarPorId(entidad.getFaja().getId());
            setValorCelda(5, 5, faja.getBloque() + "-" + faja.getNumero());
        }

        int i = 9;
        for (DetalleCenso detalle : entidad.getDetalle()) {
            //Codigo
            setValorCelda(i, 1, detalle.getCodigo());
            ///Especie
            if (detalle.getEspecie() != null) {
                Especie especie = FactoriaObjetosNegocio.getInstance().
                        getEspecieBO().recuperarPorId(detalle.getEspecie().getId());
                setValorCelda(i, 2, especie.getNombre());
            }
            //Altura
            setValorCelda(i, 3, detalle.getAltura());
            //DAP
            setValorCelda(i, 4, detalle.getDap());
            //Calidad
            if (detalle.getCalidad() != null) {
                Calidad calidad = FactoriaObjetosNegocio.getInstance().
                        getCalidadBO().recuperarPorId(detalle.getCalidad().getId());
                setValorCelda(i, 5, calidad.getCodigo());
            }
            //Condicion
            setValorCelda(i, 6, detalle.getCondicion());
            //PuntoGPS
            setValorCelda(i, 7, detalle.getPunto());
            //X
            setValorCelda(i, 8, detalle.getX());
            //Y
            setValorCelda(i, 9, detalle.getY());
            //OBs
            setValorCelda(i, 10, detalle.getObservaciones());

            i++;
        }
    }

    @Override
    protected void preparPlantillaAntesDeEnviar() {
        if (super.isCargarPlantillaFormularios()) {
            cargarAreasAPlantillaFormularios();
        } else {
            //Cargamos los datos para preparar la plantilla
            String[] valores;
            int i;
            //Areas
            setValorCelda(5, 2, area.getCodigo());
            //Fajas
            List<Faja> fajas = FactoriaObjetosNegocio.getInstance().
                    getFajaBO().obtenerFajasSegunArea(area.getId());
            valores = new String[fajas.size()];
            for (i = 0; i < fajas.size(); i++) {
                valores[i] = fajas.get(i).getBloque() + "-" + fajas.get(i).getNumero();
            }
            agregarValidacionLista(5, 5, 5, 5, valores, true, true);
            //Especies
            List<Especie> especies = FactoriaObjetosNegocio.getInstance().getEspecieBO().obtenerTodos();
            valores = new String[especies.size()];
            for (i = 0; i < especies.size(); i++) {
                valores[i] = especies.get(i).getNombre();
            }
            agregarValidacionLista(9, 38, 2, 2, valores, true, false);
            //Calidades
            List<Calidad> calidades = FactoriaObjetosNegocio.getInstance().getCalidadBO().obtenerTodos();
            valores = new String[calidades.size()];
            for (i = 0; i < calidades.size(); i++) {
                valores[i] = calidades.get(i).getCodigo();
            }
            agregarValidacionLista(9, 38, 5, 5, valores, true, false);
        }
    }

}
