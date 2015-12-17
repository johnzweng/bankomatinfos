package at.zweng.bankomatinfos.iso7816emv;

import static at.zweng.bankomatinfos.util.Utils.TAG;

import java.io.IOException;
import java.util.NoSuchElementException;

import org.simalliance.openmobileapi.Channel;
import org.simalliance.openmobileapi.Session;

import android.util.Log;
import at.zweng.bankomatinfos.exceptions.NoSmartCardException;
import at.zweng.bankomatinfos.util.Utils;

public class OmapiSessionTag implements ITag {

	private Session _sess;
	private Channel _chan;

	public OmapiSessionTag(Session session) {
		this._sess = session;
	}

	@Override
	public byte[] getId() {
		return null;
	}

	@Override
	public byte[] getHistoricalBytes() {
		return null;
	}

	@Override
	public void connectIsoDep() throws NoSmartCardException {
		// do nothing
	}

	@Override
	public void connect() throws IOException {
		// do nothing
	}

	@Override
	public void close() throws IOException {
		if (_chan != null && !_chan.isClosed()) {
			_chan.close();
		}
		// to be on the save side, let's close all channels within this session
		_sess.closeChannels();
		// and close the session
		_sess.close();
	}

	@Override
	public byte[] selectAid(byte[] aid) throws IOException {
		try {
			_chan = _sess.openLogicalChannel(aid);
			return _chan.getSelectResponse();
		} catch (NoSuchElementException nse) {
			return Utils.fromHexString("6A82");
		}
	}

	@Override
	public byte[] transceive(byte[] cmdApdu) throws IOException {
		if (_chan == null) {
			Log.w(TAG, "OmapiSessionTag: canot call transmit, channel is null. Will return 6F00.");
			return Utils.fromHexString("6F00");
		}
		return _chan.transmit(cmdApdu);
	}

}
