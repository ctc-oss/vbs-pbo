package pbos;

import java.nio.file.Paths;

import com.ctc.vht.pbos.Pbos;

/**
 *
 * @author wassj
 *
 */
public class WriterDriver {
	public static void main(final String[] args)
		throws Exception {

		Pbos.pack(Paths.get("/home/wassj/foo"), Paths.get("/home/wassj/foo1/redo.pbo"));
	}
}
