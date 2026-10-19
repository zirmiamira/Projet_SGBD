package sgbd.disk;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class DiskManager implements IDiskManager {

    private static final String DATA_FILE_NAME = "data.bin";
    private static final String META_FILE_NAME = "dm.save";
    // file containing pages
    private RandomAccessFile dbFile;
    // file containing metadata
    private File metaFile;
    private int nbrPage;
    private int pageSize;
    private List<IPageId> availablePages;
    /**
     * The Constructor
     *
     */
    public DiskManager(){
        this.nbrpage = 0;
        this.availablePages = new ArrayList<>();
    }
   
    /**
     * Initializes the disk manager with a target working directory and a page size.
     *
     * @param dmDir    the absolute path to a valid working directory.
     * @param pageSize the size of a single page in bytes.
     * @throws DiskManagerException 
     */
    public void Init(String dmDir, int pageSize) throws DiskManagerException{
    	if(dejaInitialise) {
    		throw new DiskManagerException("L'initialisation a déjà été effectuée : la méthode Init() ne peut pas être appelée une seconde fois.");
    	}
    	 checkArgumentsValidity(dmDir, pageSize);
    	 // reset attributs to avoid any confusion
    	 resetState();
    	
    	 File directory = new File(dmDir);
         File dataFile = new File(directory, DATA_FILE_NAME);
         File meta = new File(directory, META_FILE_NAME);
         
         File[] files = directory.listFiles();
         
         if (files == null) {
             throw new DiskManagerException( "Impossible de lire le contenu du dossier." );
         }
         boolean nouveauStockage = files.length == 0 || (!dataFile.exists() && !meta.exists());
         try {
             if (nouveauStockage) {
                 dataFile.createNewFile();
                 meta.createNewFile();
                 // verifier si les 2 fichiers sont crees
                 if (!dataFile.isFile() || !meta.isFile()) {
                	    throw new DiskManagerException("Impossible de créer les fichiers du DiskManager.");
                	}
                 
             } else if (!dataFile.isFile() || !meta.isFile()) {
                 throw new DiskManagerException( "Les fichiers du DiskManager sont incomplets.");
             }
             // Initialisation les attributs 
             this.metaFile = meta;
             if (nouveauStockage) {
                 this.pageSize = pageSize;
                 saveMetadata();
             } else {
                 loadMetadata();
                 if (this.pageSize != pageSize) {
                     throw new DiskManagerException( "La taille de page fournie ne correspond pas à celle enregistrée dans les métadonnées." );
                 }
             }
             this.dejaInitialise = true;
         } catch (IOException e) {
             throw new DiskManagerException("Erreur lors de l'initialisation du DiskManager.", e);
         }
     }

    /**
     * Verifier si les arguments sont bons 
     */
    private void checkArgumentsValidity(String dmDir, int pageSize) throws DiskManagerException{
        if (dmDir == null || dmDir.isBlank()) {
        	throw new DiskManagerException("Le chemin du dossier ne peut pas être vide.");
        }
        if (pageSize <= 0) {
        	throw new DiskManagerException("La taille d'une page doit être strictement positive.");
            
        }
        File directory = new File(dmDir);
        if (!directory.exists()) {
        	throw new DiskManagerException("Le dossier n'existe pas : " + dmDir);
        }
        if (!directory.isDirectory()) {
        	throw new DiskManagerException("Le chemin fourni n'est pas un dossier : " + dmDir);
        }
        
    }
    
    /**
     * Charger les métadonnées
     */
    private void loadMetadata() throws DiskManagerException {
        try {
            int size = (int) metaFile.length();
            byte[] bytes = new byte[size];

            try (FileInputStream in = new FileInputStream(metaFile)) {
                in.read(bytes);
            }
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            nbrpage = buffer.getInt();
            // a chaque fois on verifie si les donnees chargees sont coherentes
            if (nbrpage < 0) {
                throw new DiskManagerException("Le nombre de pages ne peut pas être négatif.");
            }
            pageSize = buffer.getInt();
            if (pageSize <= 0) {
                throw new DiskManagerException("La taille d'une page doit être strictement positive.");
            }
            availablePages = new ArrayList<>();
            while (buffer.hasRemaining()) {
                int index = buffer.getInt();
                if (index < 0 || index >= nbrpage) {
                    throw new DiskManagerException("Un numéro de page ne peut pas être négatif.");
                }
                availablePages.add(new PageId(index));
            }
        } catch (IOException e) {
            throw new DiskManagerException( "Erreur lors du chargement des métadonnées : " + e.getMessage());
        }
    }
    
    /**
     * Enregistrer les métadonnées
     */
    private void saveMetadata() throws DiskManagerException {
        if (nbrpage < 0) {
            throw new DiskManagerException( "Le nombre de pages ne peut pas être négatif.");
        }
        if (pageSize <= 0) {
            throw new DiskManagerException("La taille d'une page doit être strictement positive." );
        }
        int size = (2 + availablePages.size()) * Integer.BYTES;
        ByteBuffer buffer = ByteBuffer.allocate(size);

        buffer.putInt(nbrpage);
        buffer.putInt(pageSize);

        for (IPageId pageId : availablePages) {
            PageId p = (PageId) pageId;
            int index = (int) p.getIndex();

            if (index < 0 || index >= nbrpage) {
                throw new DiskManagerException("Identifiant de page invalide : " + index);
            }
            buffer.putInt((int) p.getIndex());
        }

        try (FileOutputStream out = new FileOutputStream(metaFile)) {
            out.write(buffer.array());
        } catch (IOException e) {
            throw new DiskManagerException("Erreur lors de l'écriture des métadonnées : " + e.getMessage());
        }
    }
    /**
     * Persists the current state and metadata of the disk manager
     * to the working directory.
     *
     * @throws DiskManagerException
     */
    void Save() throws DiskManagerException {
        if (!dejaInitialise) {
            throw new DiskManagerException( "Le DiskManager doit être initialisé avant de sauvegarder." );
        }
        saveMetadata();
    }

    /**
     * Allocates a new page requested by the upper layers of the DBMS.
     * @return the {@link IPageId} identifying the newly allocated page.
     * @throws DiskManagerException
     */
    IPageId AllocPage() throws DiskManagerException{
        IPageId ipid;

        //  Si une page est libre (dans la liste )
        if (!availablePages.isEmpty()) {
            ipid = availablePages.remove(0);
        } 
        // on crée un nouvel identifiant 
        else {
            ipid = new PageId(nbrpage);
            nbrPage++;
        }

        return ipid;
    }

    /**
     * Reads the on-disk content of the page designated by {@code 
     }
     * into the provided buffer.
     * <p>
     * Note: The caller is responsible for allocating and passing the destination buffer.
     *
     * @param ipid   the identifier of the page to read.
     * @param buffer the buffer into which data will be copied
     * @throws DiskManagerException
     */
    void ReadPage(IPageId ipid, ByteBuffer buffer) throws DiskManagerException;

    /**
     * Writes the content of the provided buffer into the page designated by {@code ipid}.
     * This is the counterpart of {@link #ReadPage(IPageId, ByteBuffer)}.
     *
     * @param ipid   the identifier of the target page.
     * @param buffer the buffer containing the data to write.
     * @throws DiskManagerException
     */
    void WritePage(IPageId ipid, ByteBuffer buffer) throws DiskManagerException;

    /**
     * Deallocates the page identified by {@code ipid}, marking it as available
     * for future allocations.
     *
     * @param ipid the identifier of the page to free.
     * @throws DiskManagerException
     */
    void DeallocPage(IPageId ipid) throws DiskManagerException{
        if (ipid == null) {
            throw new DiskManagerException("l'ID de la page est null");
        }

        int pageNum = ((PageId) ipid).getIndex();

        if (pageNum == 0) {
            throw new DiskManagerException(" la Page 0  est réservée aux métadonnées");
        }

        if (pageNum < 0 || pageNum >= nbrPage) {
            throw new DiskManagerException("la page " + pageNum + " n'existe pas");
        }

        if (availablePages.contains(ipid)) {
            throw new DiskManagerException("la page " + pageNum + " est déjà libre ");
        }

availablePages.add(ipid);
    }

    /**
     * Returns the total number of pages currently in use (allocated and not yet deallocated).
     *
     * @return the count of active allocated pages.
     */
    int GetActivePageCount(){
        return nbrPage-(avaiblePages.size());
    }

    /**
     * Returns the configured page size in bytes managed by this disk manager.
     *
     * @return the page size in bytes.
     */
    int GetPageSize(){
        return this.pageSize;
    }

        /**
     * Réinitialise l'état interne du DiskManager avant son initialisation.
     */
    private void resetState() throws DiskManagerException {
        try {
            if (dbFile != null) {
                dbFile.close();
            }
        } catch (IOException e) {
            throw new DiskManagerException("Impossible de fermer le fichier de données lors de la réinitialisation.");
        } finally {
            dbFile = null;
            metaFile = null;
            nbrpage = 0;
            pageSize = 0;
            availablePages = new ArrayList<>();
        }
    }
}
