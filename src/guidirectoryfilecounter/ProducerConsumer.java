/** Parallel Processing
 *  Assignment 5
 *  Created by Norman Potts
 */
package guidirectoryfilecounter;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/** Class ProducerConsumer
 *      Purpose: Moves threw a directory structure and counts every file.
 * 
 * @author Norman
 */
public class ProducerConsumer 
{ 
    /// BOUND: the constant capacity of the linkedBlockingQueue in the method 
    // startIndexing 
    private static final int BOUND = 10;
    /// N_CONSUMERS: sets the number of consumer threads that gets created to
    // the number of available processors, which is used in method startIndexing.
    private static final int N_CONSUMERS = Runtime.getRuntime().availableProcessors();    
    /// crawlerDone: starts of as false but when the crawler finishes it gets 
    // set to true. Gets its own lock called LOCK_CRAWLERDONE
    private static boolean crawlerDone;
    private static final Object LOCK_CRAWLERDONE = new Object();
    
    // New variables added for this version since assignment 3.
    private static String lookingForDis; 
    private static int countOfMatches;
    private static final Object LOCK_X = new Object();
    private static final Object LOCK_COUNTOFMATCHES = new Object();
    private static ExecutorService pool;
    private static ArrayList<String> paths_of_matched_files;
    
    
    
    
    /** Method ProducerConsumer
     *      Purpose: Runs program by calling startIndexing.
     * 
     */
    public ProducerConsumer(String path, String NameOfFileWeAreLookingFor) throws InterruptedException
    {   
        paths_of_matched_files = new ArrayList<String>();
        lookingForDis = NameOfFileWeAreLookingFor;
        countOfMatches = 0;      
        File file = new File(path);   
        File[] x = {file}; //startIndexing accepts an file array.
        pool = Executors.newFixedThreadPool(N_CONSUMERS);
        startIndexing( x );                                   
    }// End of method main.
    
    
    
    
    /** Method startIndexing
     *      Purpose: Starts the two classes, FileCrawler (consumer) and Indexer (producer)
     */
    public static void startIndexing(File[] roots) throws InterruptedException
    {   
        synchronized(LOCK_CRAWLERDONE)
        {   crawlerDone = false;    }    
        // queue: a linkedBlockingqueue the size of Bound used too hold files 
        // and directorys waiting to be consumed. 
        BlockingQueue<File> queue = new LinkedBlockingQueue<File>(BOUND);
        // filter: Determines if file is a folder or not a folder.
        FileFilter filter = new FileFilter() 
        {
            public boolean accept(File file) {   return true;    }
        };                
        // For each file in file array roots create a new thread of FileCrawler.         
        for (File root : roots)
        {                          
            pool.execute( new FileCrawler(queue, filter, root) );            
        }                
        // For the size of N_Consumers start a thread of Indexer.
        for (int i = 0; i < N_CONSUMERS; i++) 
        {
            new Thread(new Indexer(queue, i)).start();            
        }        
        // Wait for crawler to finish then check count and display it. 
        synchronized(LOCK_CRAWLERDONE)
        {
            LOCK_CRAWLERDONE.wait();
            int count = FileCrawler.countOfFiles;
            /// This program also counts shortcut files.
            System.out.println(" Count of files: "+ count);                        
            shutdownAndAwaitTermination(pool);            
        }
    }// End of method startIndexing.

    
    
    
    /** Method shutdownAndAwaitTermination
     * 
     * This method shutdown the ThreadPool.
     * 
     * Code source: From the oracle java documentation for ExecutorService.
     * URL: https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ExecutorService.html
     * 
     * @param pool 
     */
    private static void shutdownAndAwaitTermination(ExecutorService pool) {
        pool.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                pool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(60, TimeUnit.SECONDS))
                {
                    System.err.println("Pool did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }/// End of method shutdownAndAwaitTermination.
    
    
  
    
    /** Method declareCrawlisDone
     *      Purpose: Tells the consumer that crawl has finished. It does this
     *              by changing a static boolean to true. A loop in indexer's
     *              run checks this variable to see if it should exit.
     */
    public static void declareCrawlisDone(BlockingQueue<File> queue)
    {
        synchronized(LOCK_CRAWLERDONE){
            crawlerDone = true;
            LOCK_CRAWLERDONE.notifyAll();
        }           
    }// End of declareCrawlisDone.
    
    
    
    
    
    
    
    
    
    
    /** Class FileCrawler
    *      Purpose: Moves threw a directory. This is the producer.
    *
    * @author Norman
    */
    static class FileCrawler implements Runnable 
    {
        // countOfitems: Counts the items in the directory tree, gets its own 
        // lock called LOCK_COUNTOFITEMS
        private static int countOfitems = 0;
        private static final Object LOCK_COUNTOFITEMS = new Object();        
        // fileQueue: Becomes a linkedBlockingqueue the size of BOUND when
        // FileCrawler gets constructed. This Queue purpose is to ... TODO 
        private final BlockingQueue<File> fileQueue;
        // fileFilter: a Filefilter that determines which file is a folder or 
        // an actual file. 
        private final FileFilter fileFilter;
        // root: A file variable that will hold the root variable for this 
        // constuction of FileCrawler. 
        private final File root;
        // countOfFiles: A integer variable to keep count of files. This file
        // gets its own monitor.
        private static int countOfFiles;
        private static final Object LOCK_COUNTOFFILES = new Object();
        // indexedFilePaths: An arraylist of strings to remeber all the file
        // paths. 
        private static ArrayList<String> indexedFilePaths;
        private static final Object LOCK_INDEXEDFILEPATHS = new Object();
        
        
        
                        
        /** Constructor FileCrawler
         *      Purpose: Constructs a filecrawler which will determine what files 
         *               are folders or actual files using FileFilter.
         */    
        public FileCrawler(BlockingQueue<File> fileQueue, final FileFilter fileFilter, File root) 
        {             
            indexedFilePaths = new ArrayList<String>(); 
            countOfFiles = 0;
            this.fileQueue = fileQueue;
            this.root = root;
            this.fileFilter = new FileFilter()
            {
                public boolean accept(File f)
                {
                    return f.isDirectory() || fileFilter.accept(f) ;
                }
            };
        }// End of Constructor FileCrawler 
        
        
        
        
        /** Method alreadyIndexed
         *      Purpose: if file has not already been index will return false.
         *               If file has been indexed return true.
         *      How does it work? Uses an arraylist of string paths to keep track
         *          of paths that have been indexed. When a new file is analysed
         *          this method looks in the arraylist to see if the file has 
         *          already been indexed.
         */    
        private boolean alreadyIndexed(File f) 
        {   
            /// isAlreadyIndexed: A boolean variable to be set to true when this method finds out the file has already been indexed. 
            boolean isAlreadyIndexed;            
            String path = f.getPath();                                    
            ///  Is this path already in path list?             
            synchronized( LOCK_INDEXEDFILEPATHS  ){                
                if( indexedFilePaths.contains(path) == true )
                {   //Already indexed...
                    isAlreadyIndexed = true;
                }
                else
                {   //Havent been indexed..
                    indexedFilePaths.add(path);
                    isAlreadyIndexed = false;
                }
            }/// End of Synchronized LOCK_INDEXEDFILEPATHS
            return isAlreadyIndexed;                                                
        }// End of method alreadyIndexed 
        
        
        
        
        /** Method run
         *      Purpose: runs crawl method with the this root file.
         */    
        public void run() 
        {   
            try 
            {                            
                crawl(root);                
                declareCrawlisDone( this.fileQueue );                
            } catch (InterruptedException e) 
            {   Thread.currentThread().interrupt(); }
        }// End of Method run.       
        
        
        
        
        /** Method crawl
         *      Purpose: Moves threw directories, determining what is files and 
         *               what are directories.
         */    
        private void crawl(File root) throws InterruptedException 
        {                          
            //Return an array of paths denoting the directories and files.
            File[] entries = root.listFiles(fileFilter); 
            // If there are entries
            if (entries != null) 
            {
                for (File entry : entries)
                {                    
                    if (entry.isDirectory())
                    {   /// Recursivly call crawl if file is a directory.                       
                        crawl(entry); 
                    }
                    else if (!alreadyIndexed(entry))
                    {                           
                        // When entry has not already been indexed add file to
                        //  filequeue. Then index the file...
                        synchronized(LOCK_COUNTOFFILES) { countOfFiles++; }
                        fileQueue.put(entry);                      
                    }                   
                }                                                          
            }          
        }// End of Method crawl.
        
    }// End of Class FileCrawler.
    
    
    
    
    
    
    
    
    
    
    /** Class Indexer
     *      Purpose: Analysis the directories. This is the consumer.
     *
     * @author Norman
     */
    static class Indexer implements Runnable 
    {   
        private static int idexerID;
        private static final Object LOCK_INDEXERID = new Object();
        private final BlockingQueue<File> queue;
        private static Object LOCK_QUEUE = new Object();
        
        /** Constructor Indexer 
         *      Purpose: Assigns this queue of the indexer class to the one 
         *               passed into the parameter when it class gets constructed.
         */
        public Indexer(BlockingQueue<File> queue, int id) 
        {  
            synchronized(LOCK_INDEXERID){idexerID = id;}            
            this.queue = queue; //Same queue which holding files discovered in crawler.
        }    
        
        /** Method run
         *      Purpose: Calls indexFile everytime a file is at the top of the
         *              queue. Ends when the crawler is done.
         */
        public void run()
        {
            try
            {          
                /**  Create a holding variable for the status of isCrawler Done.
                 *   Use it to determine if loop should end or if the crawler 
                 *   is still working.
                 */
                boolean isCrawlerDone;
                synchronized(LOCK_CRAWLERDONE){ isCrawlerDone =  crawlerDone;}
               
                while (isCrawlerDone == false)
                {  
                    /**  Look at queue in a synchronized manner.
                     *   check to see if top of queue is null. if it null than
                     *   the producer hasn't placed any file into the queue yet.
                     *   if it is not null than the take the file and run
                     *   method indexFile on it.
                     */
                    synchronized(LOCK_QUEUE)
                    {
                        File f = queue.peek();
                        if (f != null  )
                        {   indexFile(queue.take());    }
                    }   
                    /// Check status of crawler for next loop.
                    synchronized(LOCK_CRAWLERDONE)
                    {    isCrawlerDone =  crawlerDone;  }                                
                }/// End of While loop                                
            }
            catch (InterruptedException e)
            {   Thread.currentThread().interrupt(); }
        }/// End of Method run.
        
        
        
        
        /** Method indexFile
         *      Purpose: Looks at file and sees if it matches the file name 
         *               specified by the user.
         * @param file 
         */
        public void indexFile(File file)
        {
            String nameOfItem = file.getName();
            String localNameOfX;
            synchronized(LOCK_X)
            {   localNameOfX = lookingForDis;   }                          
            
            // Using matcher and equals method, but only really nead matcher.
            String patt = ""+localNameOfX+"";            
            Pattern pattern = Pattern.compile(patt);
            Matcher m = pattern.matcher(nameOfItem);  
            boolean Matches = m.find();                                      
            if( localNameOfX.equals(nameOfItem) || Matches == true )
            {                
                synchronized(LOCK_COUNTOFMATCHES)
                {  
                    countOfMatches++;
                    paths_of_matched_files.add(file.getAbsolutePath());
                }  
            }            
        }/// End of method indexFile        
                
    }/// End of Class Indexer 
    
    
    
    
    /// Getter gets countOfMatches
    public int getCountofMatches()
    {
        synchronized(LOCK_COUNTOFMATCHES)
        {  
           return countOfMatches;
        }  
    }// End of getter.
    
    
    
    
    /// Getter gets paths_of_matched_files
    public ArrayList<String> getListOfMatches()
    {
        synchronized(LOCK_COUNTOFMATCHES)
        {  
           return paths_of_matched_files;
        }  
    }// End of getter.
    
    
}// End of Class Assignment3_PoducerConsumerApp 
