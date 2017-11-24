/**
 *
 * @author Tobias Mellstrand
 * @date 2017-11-10
 * 
 * @ref https://kebomix.wordpress.com/2011/01/09/reservoir-sampling-java/
 */

package hangman.file;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


/**
 * Helper to get a sample from the big word file.
 * Instead of going through it, twice, every time when we want a new random word
 * create a random sample of it and use that list instead.
 */
public class ReservoirSampling {
    
    private int size;
    private File file;
    
    /**
     * Constructor for the sampling class
     * 
     * @param file - file with words
     * @param size - sample size
     */
    public ReservoirSampling(File file, int size) {
	
	this.file = file;
	this.size = size;
	
    }
    
    /**
     * The a sample from the word file
     * 
     * @return - List of the sample
     * @throws FileNotFoundException - File is missing
     * @throws IOException - When reading the file goes wrong
     */
    public List<String> getSample() throws FileNotFoundException, IOException {
	
	int count = 0;
	int randomNumber;
	String currentLine;
	
	List<String> reservoirList = new ArrayList<>(size);
	BufferedReader br = new BufferedReader(new FileReader(file));
	Random ra = new Random();
	
	while((currentLine = br.readLine()) != null) {
	    count++;
	    if(count <= size) {
		reservoirList.add(currentLine);
	    } else {
		randomNumber = (int)ra.nextInt(count);
		if(randomNumber < size) {
		    reservoirList.set(randomNumber, currentLine);
		}
	    }
	}
	
	return reservoirList;
    }
}
