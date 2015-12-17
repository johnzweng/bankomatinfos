package at.zweng.bankomatinfos.iso7816emv;

import java.io.IOException;

import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.util.Log;
import at.zweng.bankomatinfos.exceptions.NoSmartCardException;

import static at.zweng.bankomatinfos.iso7816emv.EmvUtils.createSelectAid;
import static at.zweng.bankomatinfos.util.Utils.*;

public class IsoDepTag implements ITag {

	private Tag _tag;
	private IsoDep _isoDep;

	public IsoDepTag(Tag tag) {
		this._tag = tag;
	}

	@Override
	public byte[] getId() {
		return _tag.getId();
	}

	@Override
	public void connect() throws IOException {
		_isoDep.connect();
	}

	@Override
	public void close() throws IOException {
		_isoDep.close();
	}

	@Override
	public byte[] transceive(byte[] cmdApdu) throws IOException {
		return _isoDep.transceive(cmdApdu);
	}

	@Override
	public void connectIsoDep() throws NoSmartCardException {
		Log.d(TAG, "connecting Isodep");
		_isoDep = IsoDep.get(_tag);
		if (_isoDep == null) {
			throw new NoSmartCardException("This NFC tag is no ISO 7816 card");
		}
	}

	@Override
	public byte[] getHistoricalBytes() {
		return _isoDep.getHistoricalBytes();
	}

	@Override
	public byte[] selectAid(byte[] appId) throws IOException {
		byte[] command = createSelectAid(appId);
		byte[] resultPdu = _isoDep.transceive(command);
		return resultPdu;
	}

	
}
