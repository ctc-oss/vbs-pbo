package pbos;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.ctc.vht.pbos.Pbos;
import com.google.common.io.Resources;

/**
 *
 * @author wassj
 *
 */
public class StreamerDriver {
	public static void main(final String[] args)
		throws Exception {

		final Path path = Paths.get(Resources.getResource("sample.pbo").toURI());
		try (final InputStream stream = Pbos.streamFileFrom(path, "mission.sqf")) {
			System.out.println(new BufferedReader(new InputStreamReader(stream)).readLine());
		}
	}
}
