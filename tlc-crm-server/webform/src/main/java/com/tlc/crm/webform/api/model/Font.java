package com.tlc.crm.webform.api.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.webform.internal.status.WebFormErrorCodes;

/**
 * Holds the web-form {@link Font} data.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public enum Font {

    ARIAL("Arial", "https://fonts.googleapis.com/css2?family=Arimo:ital,wght@0,400..700;1,400..700&family=Bebas+Neue&family=Courier+Prime:ital,wght@0,400;0,700;1,400;1,700&family=Lato:ital,wght@0,100;0,300;0,400;0,700;0,900;1,100;1,300;1,400;1,700;1,900&family=Lexend:wght@100..900&family=Arial&display=swap", 1),
    BEBAS_NEUE("Bebas Neue", "https://fonts.googleapis.com/css2?family=Arimo:ital,wght@0,400..700;1,400..700&family=Bebas+Neue&family=Lexend:wght@100..900&display=swap", 2),
    COURIER_NEW("Courier New", "https://fonts.googleapis.com/css2?family=Arimo:ital,wght@0,400..700;1,400..700&family=Bebas+Neue&family=Courier+Prime:ital,wght@0,400;0,700;1,400;1,700&family=Lexend:wght@100..900&display=swap", 3),
    GEORGIA("Georgia", "https://fonts.googleapis.com/css2?family=Arimo:ital,wght@0,400..700;1,400..700&family=Bebas+Neue&family=Courier+Prime:ital,wght@0,400;0,700;1,400;1,700&family=Lato:ital,wght@0,100;0,300;0,400;0,700;0,900;1,100;1,300;1,400;1,700;1,900&family=Lexend:wght@100..900&family=Arial&family=Open+Sans:ital,wght@0,300;0,400;0,600;0,700;1,300;1,400;1,600;1,700&family=Georgia:ital,wght@0,400..700&display=swap", 4),
    HELVETICA("Helvetica", "https://fonts.googleapis.com/css2?family=Arimo:ital,wght@0,400..700;1,400..700&family=Bebas+Neue&family=Courier+Prime:ital,wght@0,400;0,700;1,400;1,700&family=Lato:ital,wght@0,100;0,300;0,400;0,700;0,900;1,100;1,300;1,400;1,700;1,900&family=Lexend:wght@100..900&family=Arial&family=Open+Sans:ital,wght@0,300;0,400;0,600;0,700;1,300;1,400;1,600;1,700&family=Georgia:ital,wght@0,400..700&family=Montserrat:wght@100..900&family=Roboto:wght@100..900&family=Source+Sans+Pro:wght@200..900&family=Ubuntu:wght@300..500&family=Work+Sans:wght@100..900&family=Inter:wght@100..900&family=Helvetica:ital,wght@0,300..900&display=swap", 5),
    LATO("Lato", "https://fonts.googleapis.com/css2?family=Arimo:ital,wght@0,400..700;1,400..700&family=Bebas+Neue&family=Courier+Prime:ital,wght@0,400;0,700;1,400;1,700&family=Lato:ital,wght@0,100;0,300;0,400;0,700;0,900;1,100;1,300;1,400;1,700;1,900&family=Lexend:wght@100..900&display=swap", 6),
    LEXEND("Lexend", "https://fonts.googleapis.com/css2?family=Arimo:ital,wght@0,400..700;1,400..700&family=Lexend:wght@100..900&display=swap", 7),
    MERRIWEATHER("Merriweather", "https://fonts.googleapis.com/css2?family=Arimo:ital,wght@0,400..700;1,400..700&family=Bebas+Neue&family=Courier+Prime:ital,wght@0,400;0,700;1,400;1,700&family=Lato:ital,wght@0,100;0,300;0,400;0,700;0,900;1,100;1,300;1,400;1,700;1,900&family=Lexend:wght@100..900&family=Merriweather:ital,opsz,wght@0,18..144,300..900;1,18..144,300..900&family=Playfair+Display:ital,wght@0,400..900;1,400..900&family=Poppins:ital,wght@0,100;0,200;0,300;0,400;0,500;0,600;0,700;0,800;0,900;1,100;1,200;1,300;1,400;1,500;1,600;1,700;1,800;1,900&family=Roboto+Slab:wght@100..900&family=Roboto:ital,wght@0,100..900;1,100..900&display=swap", 8),
    MONTSERRAT("Montserrat", "https://fonts.googleapis.com/css2?family=Arimo:ital,wght@0,400..700;1,400..700&family=Bebas+Neue&family=Courier+Prime:ital,wght@0,400;0,700;1,400;1,700&family=Lato:ital,wght@0,100;0,300;0,400;0,700;0,900;1,100;1,300;1,400;1,700;1,900&family=Lexend:wght@100..900&family=Arial&family=Open+Sans:ital,wght@0,300;0,400;0,600;0,700;1,300;1,400;1,600;1,700&family=Georgia:ital,wght@0,400..700&family=Montserrat:wght@100..900&display=swap", 9),
    OPEN_SANS("Open Sans", "https://fonts.googleapis.com/css2?family=Arimo:ital,wght@0,400..700;1,400..700&family=Bebas+Neue&family=Courier+Prime:ital,wght@0,400;0,700;1,400;1,700&family=Lato:ital,wght@0,100;0,300;0,400;0,700;0,900;1,100;1,300;1,400;1,700;1,900&family=Lexend:wght@100..900&family=Arial&family=Open+Sans:ital,wght@0,300;0,400;0,600;0,700;1,300;1,400;1,600;1,700&display=swap", 10),
    OSWALD("Oswald", "https://fonts.googleapis.com/css2?family=Arimo:ital,wght@0,400..700;1,400..700&family=Bebas+Neue&family=Courier+Prime:ital,wght@0,400;0,700;1,400;1,700&family=Lato:ital,wght@0,100;0,300;0,400;0,700;0,900;1,100;1,300;1,400;1,700;1,900&family=Lexend:wght@100..900&family=Merriweather:ital,opsz,wght@0,18..144,300..900;1,18..144,300..900&family=Oswald:wght@200..700&family=Playfair+Display:ital,wght@0,400..900;1,400..900&family=Poppins:ital,wght@0,100;0,200;0,300;0,400;0,500;0,600;0,700;0,800;0,900;1,100;1,200;1,300;1,400;1,500;1,600;1,700;1,800;1,900&family=Roboto+Slab:wght@100..900&family=Roboto:ital,wght@0,100..900;1,100..900&display=swap", 11),
    PLAYFLAIR_DISPLAY("Playflair Display", "https://fonts.googleapis.com/css2?family=Playfair+Display:ital,wght@0,400..900;1,400..900&display=swap", 12),
    POPPINS("Poppins", "https://fonts.googleapis.com/css2?family=Arimo:ital,wght@0,400..700;1,400..700&family=Bebas+Neue&family=Courier+Prime:ital,wght@0,400;0,700;1,400;1,700&family=Lato:ital,wght@0,100;0,300;0,400;0,700;0,900;1,100;1,300;1,400;1,700;1,900&family=Lexend:wght@100..900&family=Playfair+Display:ital,wght@0,400..900;1,400..900&family=Poppins:ital,wght@0,100;0,200;0,300;0,400;0,500;0,600;0,700;0,800;0,900;1,100;1,200;1,300;1,400;1,500;1,600;1,700;1,800;1,900&display=swap", 13),
    ROBOTO("Roboto", "https://fonts.googleapis.com/css2?family=Playfair+Display:ital,wght@0,400..900;1,400..900&family=Roboto:ital,wght@0,100..900;1,100..900&display=swap", 14),
    ROBOTO_SLAB("Roboto Slab", "https://fonts.googleapis.com/css2?family=Arimo:ital,wght@0,400..700;1,400..700&family=Bebas+Neue&family=Courier+Prime:ital,wght@0,400;0,700;1,400;1,700&family=Lato:ital,wght@0,100;0,300;0,400;0,700;0,900;1,100;1,300;1,400;1,700;1,900&family=Lexend:wght@100..900&family=Playfair+Display:ital,wght@0,400..900;1,400..900&family=Poppins:ital,wght@0,100;0,200;0,300;0,400;0,500;0,600;0,700;0,800;0,900;1,100;1,200;1,300;1,400;1,500;1,600;1,700;1,800;1,900&family=Roboto+Slab:wght@100..900&display=swap", 15),
    TIMES_NEW_ROMAN("Times New Roman", "https://fonts.googleapis.com/css2?family=Arimo:ital,wght@0,400..700;1,400..700&family=Bebas+Neue&family=Courier+Prime:ital,wght@0,400;0,700;1,400;1,700&family=Lato:ital,wght@0,100;0,300;0,400;0,700;0,900;1,100;1,300;1,400;1,700;1,900&family=Lexend:wght@100..900&family=Playfair+Display:ital,wght@0,400..900;1,400..900&family=Poppins:ital,wght@0,100;0,200;0,300;0,400;0,500;0,600;0,700;0,800;0,900;1,100;1,200;1,300;1,400;1,500;1,600;1,700;1,800;1,900&family=Roboto+Slab:wght@100..900&family=Times+New+Roman:ital,wght@0,400..700&display=swap", 16),
    VERDANA("Verdana", "https://fonts.googleapis.com/css2?family=Arimo:ital,wght@0,400..700;1,400..700&family=Bebas+Neue&family=Courier+Prime:ital,wght@0,400;0,700;1,400;1,700&family=Lato:ital,wght@0,100;0,300;0,400;0,700;0,900;1,100;1,300;1,400;1,700;1,900&family=Lexend:wght@100..900&family=Playfair+Display:ital,wght@0,400..900;1,400..900&family=Poppins:ital,wght@0,100;0,200;0,300;0,400;0,500;0,600;0,700;0,800;0,900;1,100;1,200;1,300;1,400;1,500;1,600;1,700;1,800;1,900&family=Roboto+Slab:wght@100..900&family=Times+New+Roman:ital,wght@0,400..700&family=Verdana:wght@100..900&display=swap", 17),
    ;

    private final String name;
    private final String link;
    private final int id;

    Font(final String name, final String link, final int id) {
        this.name = name;
        this.link = link;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public String getLink() {
        return link;
    }

    /**
     * Gets the {@link Font} by id.
     *
     * @param id Refers the id of the font.
     * @return The {@link Font}.
     */
    public static Font getFont(final int id) {
        return switch (id) {
            case 1 -> ARIAL;
            case 2 -> BEBAS_NEUE;
            case 3 -> COURIER_NEW;
            case 4 -> GEORGIA;
            case 5 -> HELVETICA;
            case 6 -> LATO;
            case 7 -> LEXEND;
            case 8 -> MERRIWEATHER;
            case 9 -> MONTSERRAT;
            case 10 -> OPEN_SANS;
            case 11 -> OSWALD;
            case 12 -> PLAYFLAIR_DISPLAY;
            case 13 -> POPPINS;
            case 14 -> ROBOTO;
            case 15 -> ROBOTO_SLAB;
            case 16 -> TIMES_NEW_ROMAN;
            case 17 -> VERDANA;
            default -> throw ErrorCode.get(WebFormErrorCodes.INVALID_FONT);
        };
    }
}
