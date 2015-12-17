package at.zweng.bankomatinfos.iso7816emv;

import java.io.IOException;

import at.zweng.bankomatinfos.exceptions.NoSmartCardException;

/**
 * Represents a tag like object where we can send APDUs to
 * 
 * @author <johannes@zweng.at>, 17. Dez. 2015
 */
public interface ITag {

	public byte[] getId();

	public byte[] getHistoricalBytes();

	public void connectIsoDep() throws NoSmartCardException;

	public void connect() throws IOException;

	public void close() throws IOException;

	public byte[] selectAid(byte[] aid) throws IOException;

	public byte[] transceive(byte[] cmdApdu) throws IOException;

}
