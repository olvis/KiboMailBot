/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bo.com.kibo.mailbot.impl;

import bo.com.kibo.bl.exceptions.BusinessExceptionMessage;
import bo.com.kibo.bl.impl.control.FactoriaObjetosNegocio;
import bo.com.kibo.bl.intf.IFormularioCortaBO;
import bo.com.kibo.entidades.Area;
import bo.com.kibo.entidades.Calidad;
import bo.com.kibo.entidades.Carga;
import bo.com.kibo.entidades.DetalleCorta;
import bo.com.kibo.entidades.Especie;
import bo.com.kibo.entidades.FormularioCorta;
import bo.com.kibo.entidades.Troza;
import bo.com.kibo.mailbot.intf.IInterpretadorFormularioDasometrico;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;

/**
 *
 * @author Olvinho
 */
public class InterpretadorMensajeCorta
        extends InterpretadorMensajeGenerico<FormularioCorta, Integer, IFormularioCortaBO>
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
    FormularioCorta convertirHojaEnEntidad() {
        FormularioCorta entidad = new FormularioCorta();
        Cell celda;
        //Id
        celda = getCelda(3, 3);
        if (celda.getCellType() == Cell.CELL_TYPE_NUMERIC) {
            entidad.setId((int) celda.getNumericCellValue());
        }

        //Area
        entidad.setArea(new Area());
        celda = getCelda(5, 3);
        entidad.getArea().setCodigo(getValorCeldaCadena(celda));

        //Fecha
        celda = getCelda(3, 6);
        if (DateUtil.isCellDateFormatted(celda)) {
            try {
                entidad.setFecha(celda.getDateCellValue());
            } catch (Exception e) {
                appendException(new BusinessExceptionMessage("La fecha no tiene un formato válido"));
            }
        }

        //Horas
        celda = getCelda(5, 6);
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
                DetalleCorta detalle = new DetalleCorta();
                //Codigo
                Cell celdaAux = getCelda(i, 1);
                detalle.setTroza(new Troza());
                detalle.getTroza().setCodigo(getValorCeldaCadena(celdaAux));

                //Carga
                celdaAux = getCelda(i, 2);
                if (celdaAux.getCellType() != Cell.CELL_TYPE_BLANK) {
                    detalle.setCarga(new Carga());
                    detalle.getCarga().setCodigo(getValorCeldaCadena(celdaAux));
                }

                //Especie
                celdaAux = getCelda(i, 3);
                if (celdaAux.getCellType() != Cell.CELL_TYPE_BLANK) {
                    detalle.setEspecie(new Especie());
                    detalle.getEspecie().setNombre(getValorCeldaCadena(celdaAux));
                }

                //DMayor
                celdaAux = getCelda(i, 4);
                if (celdaAux.getCellType() != Cell.CELL_TYPE_BLANK) {
                    if (celdaAux.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                        detalle.setDmayor((float) celdaAux.getNumericCellValue());
                    } else {
                        appendException(new BusinessExceptionMessage("El DMayor debe ser un valor numérico", "dMayor", fila));
                    }
                }

                //DMenor
                celdaAux = getCelda(i, 5);
                if (celdaAux.getCellType() != Cell.CELL_TYPE_BLANK) {
                    if (celdaAux.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                        detalle.setDmenor((float) celdaAux.getNumericCellValue());
                    } else {
                        appendException(new BusinessExceptionMessage("El DMenor debe ser un valor numérico", "dMenor", fila));
                    }
                }

                //Largo
                celdaAux = getCelda(i, 6);
                if (celdaAux.getCellType() != Cell.CELL_TYPE_BLANK) {
                    if (celdaAux.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                        detalle.setLargo((float) celdaAux.getNumericCellValue());
                    } else {
                        appendException(new BusinessExceptionMessage("El Largo debe ser un valor numérico", "lago", fila));
                    }
                }

                //Calidad
                celdaAux = getCelda(i, 7);
                if (celdaAux.getCellType() != Cell.CELL_TYPE_BLANK) {
                    detalle.setCalidad(new Calidad());
                    detalle.getCalidad().setCodigo(getValorCeldaCadena(celdaAux));
                }

                //Obs
                celdaAux = getCelda(i, 8);
                detalle.setObservaciones(getValorCeldaCadena(celdaAux));

                entidad.getDetalle().add(detalle);
            }
            i++;
        } while (celda.getCellType() != Cell.CELL_TYPE_BLANK);

        return entidad;
    }

    @Override
    IFormularioCortaBO getObjetoNegocio() {
        return FactoriaObjetosNegocio.getInstance().getFormularioCortaBO();
    }

    @Override
    boolean esNuevo(FormularioCorta entidad) {
        return (entidad.getId() == null);
    }

    @Override
    String getId(FormularioCorta entidad) {
        return entidad.getId().toString();
    }

    @Override
    Integer convertirId(String cadena) throws Exception {
        return convertirIdAEntero(cadena);
    }

    @Override
    void mostrarLista(List<FormularioCorta> lista) {
        int i = 5;
        for (FormularioCorta f : lista) {
            setValorCelda(i, 1, f.getId());
            setValorCelda(i, 2, f.getFecha());
            setValorCelda(i, 3, FactoriaObjetosNegocio.getInstance().
                    getAreaBO().getCodigo(f.getArea().getId()));
            i++;
        }
    }

    @Override
    void mostrarEntidad(FormularioCorta entidad) {
        setValorCelda(3, 3, entidad.getId());
        setValorCelda(3, 6, entidad.getFecha());
        setValorCelda(5, 6, entidad.getHoras());

        setValorCelda(5, 3, FactoriaObjetosNegocio.getInstance().getAreaBO().
                getCodigo(entidad.getArea().getId()));

        int i = 9;
        for (DetalleCorta detalle : entidad.getDetalle()) {
            //Codigo
            if (detalle.getTroza() != null) {
                setValorCelda(i, 1,
                        FactoriaObjetosNegocio.getInstance().getTrozaBO().getCodigo(detalle.getTroza().getNumero()));
            }
            //Carga
            if (detalle.getCarga() != null){
                setValorCelda(i, 2,
                        FactoriaObjetosNegocio.getInstance().getCargaBO().getCodigo(detalle.getCarga().getId()));
            }
            ///Especie
            if (detalle.getEspecie() != null) {
                Especie especie = FactoriaObjetosNegocio.getInstance().
                        getEspecieBO().recuperarPorId(detalle.getEspecie().getId());
                setValorCelda(i, 3, especie.getNombre());
            }
            
            //DMayor
            setValorCelda(i, 4, detalle.getDmayor());
            
            //DMenor
            setValorCelda(i, 5, detalle.getDmenor());
            
            //Largo
            setValorCelda(i, 6, detalle.getLargo());

            
            //Calidad
            if (detalle.getCalidad() != null) {
                Calidad calidad = FactoriaObjetosNegocio.getInstance().
                        getCalidadBO().recuperarPorId(detalle.getCalidad().getId());
                setValorCelda(i, 7, calidad.getCodigo());
            }
            //OBs
            setValorCelda(i, 8, detalle.getObservaciones());

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
            setValorCelda(5, 3, area.getCodigo());

            //Arboles
            List<Troza> trozas = FactoriaObjetosNegocio.getInstance().getTrozaBO()
                    .getTrozasParaCorta(area.getId());
            valores = new String[trozas.size()];
            for (i = 0; i < trozas.size(); i++) {
                valores[i] = trozas.get(i).getCodigo();
            }
            agregarValidacionLista(9, 38, 1, 1, valores, true, false);

            //Carga
            List<Carga> cargas = FactoriaObjetosNegocio.getInstance().getCargaBO().obtenerTodos();
            valores = new String[cargas.size()];
            for (i = 0; i < cargas.size(); i++) {
                valores[i] = cargas.get(i).getCodigo();
            }
            agregarValidacionLista(9, 38, 2, 2, valores, true, false);

            //Especies
            List<Especie> especies = FactoriaObjetosNegocio.getInstance().getEspecieBO().obtenerTodos();
            valores = new String[especies.size()];
            for (i = 0; i < especies.size(); i++) {
                valores[i] = especies.get(i).getNombre();
            }
            agregarValidacionLista(9, 38, 3, 3, valores, true, false);

            //Calidades
            List<Calidad> calidades = FactoriaObjetosNegocio.getInstance().getCalidadBO().obtenerTodos();
            valores = new String[calidades.size()];
            for (i = 0; i < calidades.size(); i++) {
                valores[i] = calidades.get(i).getCodigo();
            }
            agregarValidacionLista(9, 38, 7, 7, valores, true, false);

        }
    }

}
