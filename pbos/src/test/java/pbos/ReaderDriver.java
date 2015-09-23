package pbos;

import java.nio.file.Paths;

import com.ctc.vht.pbos.Pbos;

/**
 *
 * @author wassj
 *
 */
public class ReaderDriver {
	public static void main(final String[] args)
		throws Exception {

		Pbos.unpack(Paths.get(ReaderDriver.class.getResource("/sample.pbo").toURI()), Paths.get("/home/wassj/foo"));
	}
}
