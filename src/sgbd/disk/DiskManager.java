package sgbd.disk;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

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
    // check if init() is initilazed
    private boolean isInitialized = false;
	
    /**
     * The Constructor
     *
     */
    public DiskManager(){
        this.nbrPage = 0;
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
    	if(isInitialized) {
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
             this.isInitialized = true;
          } catch (DiskManagerException e) {
        	    resetState();
        	    throw e;
        	} catch (IOException e) {
        	    resetState();
        	    throw new DiskManagerException("Erreur lors de l'initialisation du DiskManager.", e);
        	}
     }

    /**
     * check if the arguments are correct
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
     * Charge metadata
     */
        private void loadMetadata() throws DiskManagerException {
        try {
        	// Lire le fichier complet
	        byte[] bytes = Files.readAllBytes(metaFile.toPath());
	
	        // Il faut au minimum 2 entiers (nbrPage et pageSize)
	        if (bytes.length < 2 * Integer.BYTES) {
	            throw new DiskManagerException("Fichier de métadonnées trop court.");
	        }
	        // Après les 8 premiers octets, chaque index doit occuper exactement 4 octets vu que c'est un int
	        int remainingBytes = bytes.length - 2 * Integer.BYTES;
	        if (remainingBytes % Integer.BYTES != 0) {
	            throw new DiskManagerException("Fichier de métadonnées tronqué : un index de page est incomplet.");
	        }
	        ByteBuffer buffer = ByteBuffer.wrap(bytes);
	        
            nbrPage = buffer.getInt();
            // a chaque fois on verifie si les donnees chargees sont coherentes
            if (nbrPage < 0) {
            	 nbrPage=0;
                throw new DiskManagerException("Le nombre de pages ne peut pas être négatif.");
            }
            pageSize = buffer.getInt();
            if (pageSize <= 0) {
            	pageSize=0;
                throw new DiskManagerException("La taille d'une page doit être strictement positive.");
            }
            availablePages = new ArrayList<>();
            while (buffer.hasRemaining()) {
                int index = buffer.getInt();
                if (index < 0 || index >= nbrPage) {
                	availablePages= null;
                    throw new DiskManagerException(" Index de page libre invalide : "+ index);
                }
                availablePages.add(new PageId(index));
            }
        } catch (DiskManagerException e) {
            throw e;
        } catch (IOException e) {
            throw new DiskManagerException(
                "Erreur lors de la lecture des métadonnées.", e
            );
        }
    }
    
    /**
     * save metadata
     */
    private void saveMetadata() throws DiskManagerException {
        if (nbrPage < 0) {
            throw new DiskManagerException( "Le nombre de pages ne peut pas être négatif.");
        }
        if (pageSize <= 0) {
            throw new DiskManagerException("La taille d'une page doit être strictement positive." );
        }
        int size = (2 + availablePages.size()) * Integer.BYTES;
        ByteBuffer buffer = ByteBuffer.allocate(size);

        buffer.putInt(nbrPage);
        buffer.putInt(pageSize);

        for (IPageId pageId : availablePages) {
            PageId p = (PageId) pageId;
            int index = (int) p.getIndex();

            if (index < 0 || index >= nbrPage) {
                throw new DiskManagerException("Identifiant de page invalide : " + index);
            }
            buffer.putInt((int) p.getIndex());
        }

        try (FileOutputStream out = new FileOutputStream(metaFile)) {
            out.write(buffer.array());
        } catch (Exception e) {
            throw new DiskManagerException("Erreur lors de l'écriture des métadonnées : " + e.getMessage());
        }
    }
	
    /**
     * Persists the current state and metadata of the disk manager
     * to the working directory.
     *
     * @throws DiskManagerException
     */
    public void Save() throws DiskManagerException {
        if (!isInitialized) {
            throw new DiskManagerException( "Le DiskManager doit être initialisé avant de sauvegarder." );
        }
        try {
        saveMetadata();
        }catch(Exception e) {
        	throw new DiskManagerException("Erreur lors de la sauvegarde des métadonnées : " + e.getMessage());
        }
    }
	
    /**
     * Allocates a new page requested by the upper layers of the DBMS.
     * @return the {@link IPageId} identifying the newly allocated page.
     * @throws DiskManagerException
     */
		public IPageId AllocPage() throws DiskManagerException {
		    if (!isInitialized) {
		        throw new DiskManagerException("Le DiskManager doit être initialisé." );
		    }
		
		    IPageId ipid = null;
            int ancienNbrPage = nbrPage;
            List<IPageId> anciennesPagesLibres = new ArrayList<>(availablePages);
		    try {
		        File dataFile = new File(metaFile.getParentFile(), DATA_FILE_NAME);
		        try (RandomAccessFile raf = new RandomAccessFile(dataFile, "rw")) {
		            if (!availablePages.isEmpty()) {
		                // Réutiliser une page déjà libérée
		                ipid = availablePages.remove(0);
		            } else {
		                // Allouer une nouvelle page
		                ipid = new PageId(nbrPage);
		                nbrPage++;
		            }
		            // Réserver physiquement l'espace pour toutes les pages
		            long requiredLength = (long) nbrPage * pageSize;
		            if (raf.length() < requiredLength) {
		                raf.setLength(requiredLength);
		            }
		        }
            } catch (IOException e) {
                throw new DiskManagerException("Erreur lors de l'allocation de la page.", e);
            }
            try {
                Save();
            } catch (DiskManagerException e) {
                availablePages = anciennesPagesLibres;
                nbrPage = ancienNbrPage;
                throw new DiskManagerException( "La sauvegarde a échoué après l'allocation. L'état en mémoire et la taille de data.bin ont été restaurés.",e);
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
    @Override
    public void ReadPage(IPageId ipid, ByteBuffer buffer) throws DiskManagerException {
        
        checkArgumentsValidity(ipid);
        
        int pageIndex = ((PageId) ipid).getIndex();

        if (buffer == null || buffer.capacity() != pageSize) {
            throw new DiskManagerException("Le buffer doit avoir une capacité exactement égale à "+ pageSize + " octets.");
        }


        File dataFile = new File(metaFile.getParentFile(), DATA_FILE_NAME);

        try (RandomAccessFile raf = new RandomAccessFile(dataFile, "r")) {
            raf.seek((long) pageIndex * pageSize);
            byte[] data = new byte[pageSize];
            raf.readFully(data);
            buffer.clear();
            buffer.put(data);
        } catch (FileNotFoundException e) {
            throw new DiskManagerException(
                    "Fichier introuvable ou accès en lecture refusé lors de la lecture de la page "+ pageIndex,e);
        } catch (EOFException e) {
            throw new DiskManagerException(
                    "La page " + pageIndex + " n'a pas pu être lue en entier", e);
        } catch (IOException e) {
            throw new DiskManagerException(
                    "Erreur matérielle de lecture de la page " + pageIndex + " : " + e, e);
        }catch (Exception e) {
            throw new DiskManagerException(
                    "Erreur lors de la lecture de la page ", e);
        }
    }

    @Override
	/**
     * Writes the content of the provided buffer into the page designated by {@code ipid}.
     * This is the counterpart of {@link #ReadPage(IPageId, ByteBuffer)}.
     *
     * @param ipid   the identifier of the target page.
     * @param buffer the buffer containing the data to write.
     * @throws DiskManagerException
     */
    public void WritePage(IPageId ipid, ByteBuffer buffer) throws DiskManagerException {
       
        checkArgumentsValidity(ipid);
        int pageIndex = ((PageId) ipid).getIndex();
        
        if (buffer == null || buffer.capacity() != pageSize) {
            throw new DiskManagerException("Le buffer doit avoir une capacité exactement égale à "+ pageSize + " octets.");
        }

        File dataFile = new File(metaFile.getParentFile(), DATA_FILE_NAME);

    	try (RandomAccessFile raf = new RandomAccessFile(dataFile, "rw")) {
    	    raf.seek((long) pageIndex * pageSize);

    	    byte[] data = new byte[pageSize];

    	    buffer.clear();
    	    buffer.get(data);

    	    raf.write(data);
    	}
        catch (FileNotFoundException e) {
            throw new DiskManagerException(
                    "Fichier introuvable ou accès en écriture refusé pour la page " + pageIndex, e);
        } catch (IOException e) {
            throw new DiskManagerException(
                    "Erreur matérielle lors de l'écriture de la page " + pageIndex + " sur le disque dur.", e);
        }catch (Exception e) {
            throw new DiskManagerException(
                    "Erreur lors de l'écriture de la page ", e);
        }
    }
	/**
     * Deallocates the page identified by {@code ipid}, marking it as available
     * for future allocations.
     *
     * @param ipid the identifier of the page to free.
     * @throws DiskManagerException
     */
    public void DeallocPage(IPageId ipid) throws DiskManagerException {
        checkArgumentsValidity(ipid);

        availablePages.add(ipid);
        Save();
    }
    /**
     * Check the new id of a page befor read/write or dealloc
     *
     * @param ipid the identifier of the page to free.
     * @throws DiskManagerException
     */
    private void checkArgumentsValidity(IPageId ipid) throws DiskManagerException{
         if (isInitialized == false) {
            throw new DiskManagerException("Le DiskManager n'est pas initialisé. Appelez Init() en premier.");
        }
        if (ipid == null) {
            throw new DiskManagerException("L'identifiant de page ne peut pas être null.");
        }

        int pageIndex = ((PageId) ipid).getIndex();

        if (pageIndex < 0) {
            throw new DiskManagerException("L'ID de la page est négatif (" + pageIndex + ").");
        }    

        if (pageIndex >= nbrPage) {
            throw new DiskManagerException(
                    "La page " + pageIndex + " n'a jamais été créée (limite actuelle : " + nbrPage + ").");
        }    

        if (availablePages.contains(ipid)) {
            throw new DiskManagerException(
                    "La page " + pageIndex + " a déjà été désallouée");
        }
        
    }

    /**
     * Returns the total number of pages currently in use (allocated and not yet deallocated).
     *
     * @return the count of active allocated pages.
     */
    public int GetActivePageCount(){
        return nbrPage-(availablePages.size());
    }

    /**
     * Returns the configured page size in bytes managed by this disk manager.
     *
     * @return the page size in bytes.
     */
    public int GetPageSize(){
        return this.pageSize;
    }
    /**
     * Returns information about the actual DiskManager
     *
     * @return string containing infos
     */
     @Override
    public String toString() {
        if (!isInitialized) {
            return "DiskManager{non initialisé}";
        }
        return "DiskManager{pageSize=" + pageSize+ ", nbrpage=" + nbrPage + ", pagesLibres=" + availablePages + "}";
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
            nbrPage = 0;
            pageSize = 0;
            isInitialized=false;
            availablePages = new ArrayList<>();
        }
    }
    
    
    
}
