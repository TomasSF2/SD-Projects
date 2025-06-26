package fctreddit.impl.imgur;

public record AddImagesToAlbumArguments(String[] ids, String[] deletehashes) {

	public AddImagesToAlbumArguments(String imageId) {
		this(new String[]{imageId} , null);
	}
	
}