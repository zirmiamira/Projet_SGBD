package sgbd.buffer;
import java.nio.ByteBuffer;
import sgbd.common.DBMSException;
import sgbd.disk.IDiskManager;
import sgbd.disk.IPageId;

public  class BufferManager implements IBufferManager {
	
	
	    private int pageSize;
	    private int bufferCount;
	    private ReplacementPolicy policy;
	    private IDiskManager diskManager;
	    private Frame[] bufferPool;
	    private boolean isInitialized = false;

    // Structure interne pour modéliser une case mémoire
	private class Frame {
	    /*Identifiant de la page actuellement stockée dans cette frame*/
	    IPageId pageId = null;
	    /** Nombre de fois où la page est actuellement utilisée*/
	    int pinCount = 0;
	    /*Indique si la page a été modifiée en mémoire et doit être sauvegardée sur disque.*/
	    boolean dirtyFlag = false;
	    /* Buffer mémoire contenant les données de la page. */
	    ByteBuffer buffer;
	    /* Instant du dernier passage du pinCount à 0.*/
	    long t = 0;
	    /**
	     * Construit une frame avec un buffer mémoire de la taille d'une page.
	     * @param pageSize Taille du buffer en octets.
	     */
	    public Frame(int pageSize) {
	        this.buffer = ByteBuffer.allocate(pageSize);
	    }
	}

   

    
	 /**
     * Initializes the buffer manager with the specified configurations and components.
     *
     * @param pageSize    The size of a page in bytes.
     * @param bufferCount The number of buffer frames managed by the buffer manager.
     * @param policy      The initial page replacement policy (e.g., LRU, MRU).
     * @param diskManager The underlying disk manager instance used for disk I/O.
     * @throws DBMSException
     */
    @Override
    public void init(int pageSize, int bufferCount, ReplacementPolicy policy, IDiskManager diskManager) throws DBMSException {
        // Vérifier si le BufferManager est déjà initialisé
        if (isInitialized) {
            throw new DBMSException("Le BufferManager est déjà initialisé.");
        }

        // Vérifier les paramètres
        if (pageSize <= 0) {
            throw new DBMSException("La taille d'une page doit être strictement positive.");
        }
        if (bufferCount <= 0) {
            throw new DBMSException("Le nombre de frames doit être strictement positif.");
        }
        if (policy == null) {
            throw new DBMSException("La politique de remplacement ne peut pas être null.");
        }
        if (diskManager == null) {
            throw new DBMSException("Le DiskManager ne peut pas être null.");
        }

        // Vérifier la cohérence avec le DiskManager
        if (diskManager.GetPageSize() != pageSize) {
            throw new DBMSException("La taille des pages ne correspond pas à celle du DiskManager.");
        }

        // Initialisation des attributs
        this.pageSize = pageSize;
        this.bufferCount = bufferCount;
        this.policy = policy;
        this.diskManager = diskManager;

        this.bufferPool = new Frame[bufferCount];
        for (int i = 0; i < bufferCount; i++) {
            this.bufferPool[i] = new Frame(pageSize);
        }
        this.isInitialized = true;
    }
    
    
    
    /**
     * Responds to a page request from upper layers.
     * Retrieves the buffer containing the requested page.
     *
     * @param pageId The identifier of the requested page.
     * @return The buffer containing the page data.
     * @throws DBMSException
     */
    public ByteBuffer getPage(IPageId pageId) throws DBMSException {
        // 1. Vérifications de base
        if (!isInitialized) {
            throw new DBMSException("Le BufferManager n'est pas initialisé.");
        }
        if (pageId == null) {
            throw new DBMSException("L'identifiant de page ne peut pas être null.");
        }

        // 2. Cas 1 (Buffer Hit) : La page est déjà chargée en RAM
        for (Frame frame : bufferPool) {
            if (frame.pageId != null && frame.pageId.equals(pageId)) {
                frame.pinCount++;
                return frame.buffer;
            }
        }

        // 3. Cas 2 (Buffer Miss) : Chercher d'abord une frame vide (jamais utilisée)
        Frame targetFrame = null;
        for (Frame frame : bufferPool) {
            if (frame.pageId == null) {
                targetFrame = frame;
                break;
            }
        }

        // 4. Cas 3 : Si aucune frame n'est vide, appliquer la politique de remplacement (LRU ou MRU)
        // On cherche uniquement parmi les frames libres (pinCount == 0)
        if (targetFrame == null) {
            for (Frame frame : bufferPool) {
                if (frame.pinCount == 0) {
                    if (targetFrame == null) {
                        targetFrame = frame;
                    } else {
                        if (this.policy == ReplacementPolicy.LRU) {
                            // LRU : on prend la frame libérée le plus anciennement (plus petit t)
                            if (frame.t < targetFrame.t) {
                                targetFrame = frame;
                            }
                        } else if (this.policy == ReplacementPolicy.MRU) {
                            // MRU : on prend la frame libérée le plus récemment (plus grand t)
                            if (frame.t > targetFrame.t) {
                                targetFrame = frame;
                            }
                        }
                    }
                }
            }
        }

        // Si toutes les frames ont un pinCount > 0, impossible de remplacer
        if (targetFrame == null) {
            throw new DBMSException("Buffer pool saturé : aucune frame disponible (toutes ont pinCount > 0).");
        }

        // 5. Si la frame victime contenait une page modifiée, on la sauvegarde sur disque
        if (targetFrame.pageId != null && targetFrame.dirtyFlag) {
            diskManager.WritePage(targetFrame.pageId, targetFrame.buffer);
            targetFrame.dirtyFlag = false;
        }

        // 6. Chargement de la nouvelle page depuis le disque dans le buffer de la frame
        targetFrame.buffer.clear(); // Remet position = 0 et limit = capacity avant lecture
        diskManager.ReadPage(pageId, targetFrame.buffer);

        // 7. Mise à jour des métadonnées de la frame
        targetFrame.pageId = pageId;
        targetFrame.pinCount = 1;
        targetFrame.dirtyFlag = false;

        return targetFrame.buffer;
    }

    
    /**
     * Releases a previously requested page.
     *
     * @param pageId   The identifier of the page to free.
     * @param valDirty Whether the page content has been modified.
     * @throws DBMSException
     */
    public void freePage(IPageId pageId, boolean valDirty) throws DBMSException{
    	
    }

    /**
     * Updates the current replacement policy.
     * Overrides the policy previously configured.
     *
     * @param policy The new replacement policy to use.
     * @throws DBMSException
     */
    public void setPolicy(ReplacementPolicy policy) throws DBMSException{
    	
    }

    /**
     * Saves all modified pages (dirty == true/1) to disk via the DiskManager,
     *
     * @throws DBMSException
     */
   public  void saveBuffers() throws DBMSException{
	   
   }

    /**
     * Retrieves the current pin count of the specified page in the buffer pool.
     *
     * @param pageId The identifier of the page.
     * @return The pin count of the page if the page is in the buffer pool
     * @throws DBMSException
     */
    public  int getPinCount(IPageId pageId) throws DBMSException{
    	return 0;
    }

    /**
     * Retrieves the dirty status of the specified page in the buffer pool.
     *
     * @param pageId The identifier of the page.
     * @return Rhe dirty flag of the page if the page is in the buffer pool 
     * @throws DBMSException
     */
    public  boolean getDirtyFlag(IPageId pageId) throws DBMSException{
    	return false;
    	}
}

}
