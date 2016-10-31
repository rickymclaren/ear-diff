import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class EarDiff {

	JarFile[] ears = new JarFile[2];
	long startTime;
	long elapsed;
	
	public EarDiff(String file1, String file2) throws IOException {
		ears[0] = new JarFile(file1);
		ears[1] = new JarFile(file2);
		startTime = System.currentTimeMillis();
		
	}

	/**
	 * Remove the CRC which is after the last colon
	 * 
	 * @param codes
	 * @return
	 */
	public List<String> stripCRCs(List<String> codes) {
		List<String> result = new ArrayList<String>();
		for (String s : codes) {
			result.add(s.substring(0, s.lastIndexOf(':')));
		}
		return result;
	}

	/**
	 * Extract an embedded jar file so that it can be scanned.
	 * 
	 * @param jar
	 * @param jarEntry
	 * @param prefix
	 * @return
	 * @throws IOException
	 */
	public List<String> dumpEmbeddedJar(JarFile jar, JarEntry jarEntry, String prefix) throws IOException {
		
		prefix += jarEntry.getName() + ":";
		
		File tmpFile = File.createTempFile("JAR", "jar");
		byte[] buffer = new byte[4096];
		InputStream is = jar.getInputStream(jarEntry);
		OutputStream os = new FileOutputStream(tmpFile);
		int length = 0;
		while ((length = is.read(buffer)) != -1) {
			os.write(buffer, 0, length);
		}
		os.close();
		is.close();
		
		return dumpJar(new JarFile(tmpFile), prefix);
		
	}
	
	/**
	 * Scan a jar file. If it is an embedded jar then pass a prefix.
	 * 
	 * @param jar
	 * @param prefix
	 * @return
	 * @throws IOException
	 */
	public List<String> dumpJar(JarFile jar, String prefix) throws IOException {
		
		List<String> codes = new ArrayList<String>();
		for (JarEntry entry : Collections.list(jar.entries())) {
			codes.add(prefix + entry.getName() + ":" + entry.getCrc());
			
			if (entry.getName().toLowerCase().endsWith(".war") || entry.getName().toLowerCase().endsWith(".jar")) {
				codes.addAll(dumpEmbeddedJar(jar, entry, prefix));
				
			}
			
		}
		
		return codes;
		
	}
	
	/**
	 * Compare two ear files.
	 * 
	 * @return true if files are identical
	 */
	public boolean compare() throws IOException {
		
		List<String> codes1 = dumpJar(ears[0], "");
		List<String> codes2 = dumpJar(ears[1], "");
		
		// Throw away codes that have identical filenames and CRCs.
		List<String> identical = new ArrayList<String>(codes1);
		identical.retainAll(codes2);
		codes1.removeAll(identical);
		codes2.removeAll(identical);
		System.out.println(identical.size() + " identical files found");
		
		// Remove the CRCs from the files that are left
		codes1 = stripCRCs(codes1);
		codes2 = stripCRCs(codes2);
		
		// If they are in both ears then the file has been changed.
		// Otherwise it has been added/removed
		List<String> changed = new ArrayList<String>(codes1);
		changed.retainAll(codes2);
		codes1.removeAll(changed);
		codes2.removeAll(changed);
		
		System.out.println("CHANGED:");
		for (String code : changed) {
			System.out.println(code);
		}
		System.out.println("Only in EAR1:");
		for (String code : codes1) {
			System.out.println(code);
		}
		System.out.println("Only in EAR2:");
		for (String code : codes2) {
			System.out.println(code);
		}
		
		elapsed = System.currentTimeMillis() - startTime;
		
		return (codes1.size() + codes2.size() + changed.size() == 0);
	}
	
	public long getElapsedTime() { 
		return elapsed;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		boolean same = true;
		long elapsed = 0;
		try {
			if (args != null && args.length == 2) {
				System.out.println("Comparing " + args[0] + " against " + args[1]);
				EarDiff diff = new EarDiff(args[0], args[1]);
				same = diff.compare();
				elapsed = diff.getElapsedTime();
				
				
			} else {
				System.out.println("Usage EarDiff <file1> <file2>");
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			
		} finally {
			System.out.println("Done " + (same ? "- no differences found" : "") + " in " + elapsed + " ms.");
			
		}

	}

}
