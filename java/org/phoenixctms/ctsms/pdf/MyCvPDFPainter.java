package org.phoenixctms.ctsms.pdf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.phoenixctms.ctsms.pdf.MyCvPDFBlock.BlockType;
import org.phoenixctms.ctsms.util.CommonUtil;
import org.phoenixctms.ctsms.util.CoreUtil;
import org.phoenixctms.ctsms.util.L10nUtil;
import org.phoenixctms.ctsms.util.L10nUtil.Locales;
import org.phoenixctms.ctsms.util.Settings;
import org.phoenixctms.ctsms.util.Settings.Bundle;
import org.phoenixctms.ctsms.vo.CvPDFVO;
import org.phoenixctms.ctsms.vo.CvPositionPDFVO;
import org.phoenixctms.ctsms.vo.CvSectionVO;
import org.phoenixctms.ctsms.vo.StaffAddressOutVO;
import org.phoenixctms.ctsms.vo.StaffImageOutVO;
import org.phoenixctms.ctsms.vo.StaffOutVO;

public class MyCvPDFPainter extends org.phoenixctms.ctsms.pdf.CVPDFPainter {

	private int blockIndex;
	private ArrayList<MyCvPDFBlock> blocks;
	private MyCvPDFBlockCursor cursor;
	private HashMap<Long, PDFJpeg> images;
	private CvPDFVO pdfVO;
	private Collection<StaffOutVO> staffVOs;
	private Collection<CvSectionVO> allCvSectionVOs;
	private HashMap<Long, HashMap<Long, Collection<CvPositionPDFVO>>> cvPositionVOMap;
	private HashMap<Long, StaffAddressOutVO> addressVOMap;
	private HashMap<Long, StaffImageOutVO> imageVOMap;
	private float pageWidth;
	private float pageHeight;
	private PDFont fontA;
	private PDFont fontB;
	private PDFont fontC;
	private final static PDRectangle DEFAULT_PAGE_SIZE = PDPage.PAGE_SIZE_A4;
	private static final String CV_PDF_FILENAME_PREFIX = "cv_";

	public MyCvPDFPainter() {
		super();
		blocks = new ArrayList<MyCvPDFBlock>();
		images = new HashMap<Long, PDFJpeg>();
		pdfVO = new CvPDFVO();
		cursor = new MyCvPDFBlockCursor(this);
		setDrawPageNumbers(Settings.getBoolean(MyCvPDFSettingCodes.SHOW_PAGE_NUMBERS, Bundle.CV_PDF, MyCvPDFDefaultSettings.SHOW_PAGE_NUMBERS));
	}

	@Override
	public void drawNextBlock(PDPageContentStream contentStream) throws Exception {
		MyCvPDFBlock block = blocks.get(blockIndex);
		cursor.setBlockY(cursor.getBlockY() - block.renderBlock(contentStream, cursor));
		blockIndex++;
	}

	@Override
	public void drawPage(PDPageContentStream contentStream) throws Exception {
		// PDFUtil.renderFrame(contentStream, FRAME_COLOR, Settings.getFloat(MyCvPDFSettingCodes.PAGE_LEFT_MARGIN, Bundle.CV_PDF, MyCvPDFDefaultSettings.PAGE_LEFT_MARGIN),
		// Settings.getFloat(MyCvPDFSettingCodes.PAGE_LOWER_MARGIN, Bundle.CV_PDF, MyCvPDFDefaultSettings.PAGE_LOWER_MARGIN), pageWidth -
		// Settings.getFloat(MyCvPDFSettingCodes.PAGE_LEFT_MARGIN, Bundle.CV_PDF, MyCvPDFDefaultSettings.PAGE_LEFT_MARGIN) - Settings.getFloat(MyCvPDFSettingCodes.PAGE_RIGHT_MARGIN,
		// Bundle.CV_PDF, MyCvPDFDefaultSettings.PAGE_RIGHT_MARGIN), pageHeight - PAGE_UPPER_MARGIN - Settings.getFloat(MyCvPDFSettingCodes.PAGE_LOWER_MARGIN, Bundle.CV_PDF,
		// MyCvPDFDefaultSettings.PAGE_LOWER_MARGIN), PDFUtil.Alignment.BOTTOM_LEFT, PAGE_FRAME_LINE_WIDTH);
		if (cursor.getStaff() != null) {
			// MyCvPDFBlock block = blocks.get(blockIndex - 1);
			PDFUtil.renderTextLine(
					contentStream,
					fontA,
					PDFUtil.FontSize.TINY,
					Settings.getColor(MyCvPDFSettingCodes.TEXT_COLOR, Bundle.CV_PDF, MyCvPDFDefaultSettings.TEXT_COLOR),
					L10nUtil.getCVPDFLabel(Locales.CV_PDF, MyCvPDFLabelCodes.FOOTER_NAME, "", cursor.getStaff().getNameWithTitles()),
					Settings.getFloat(MyCvPDFSettingCodes.PAGE_LEFT_MARGIN, Bundle.CV_PDF, MyCvPDFDefaultSettings.PAGE_LEFT_MARGIN),
					Settings.getFloat(MyCvPDFSettingCodes.PAGE_LOWER_MARGIN, Bundle.CV_PDF, MyCvPDFDefaultSettings.PAGE_LOWER_MARGIN), PDFUtil.Alignment.BOTTOM_LEFT);
		}
	}

	@Override
	protected void drawPageNumber(PDFImprinter writer, PDPage page, int pageNumber, int totalPages) throws IOException {
		PDPageContentStream contentStream = writer.openContentStream(page);
		// PDFUtil.renderTextLine(
		// contentStream,
		// fontA,
		// PDFUtil.FontSize.TINY,
		// Settings.getColor(MyCvPDFSettingCodes.TEXT_COLOR, Bundle.CV_PDF, MyCvPDFDefaultSettings.TEXT_COLOR),
		// L10nUtil.getCVPDFLabel(Locales.CV_PDF, MyCvPDFLabelCodes.PAGE_NUMBER, "", pageNumber, totalPages),
		// Settings.getFloat(MyCvPDFSettingCodes.PAGE_LEFT_MARGIN, Bundle.CV_PDF, MyCvPDFDefaultSettings.PAGE_LEFT_MARGIN)
		// + (pageWidth - Settings.getFloat(MyCvPDFSettingCodes.PAGE_LEFT_MARGIN, Bundle.CV_PDF, MyCvPDFDefaultSettings.PAGE_LEFT_MARGIN) - Settings.getFloat(
		// MyCvPDFSettingCodes.PAGE_RIGHT_MARGIN, Bundle.CV_PDF, MyCvPDFDefaultSettings.PAGE_RIGHT_MARGIN)) / 2.0f,
		// Settings.getFloat(MyCvPDFSettingCodes.PAGE_LOWER_MARGIN, Bundle.CV_PDF, MyCvPDFDefaultSettings.PAGE_LOWER_MARGIN), PDFUtil.Alignment.BOTTOM_CENTER);
		PDFUtil.renderTextLine(
				contentStream,
				fontA,
				PDFUtil.FontSize.TINY,
				Settings.getColor(MyCvPDFSettingCodes.TEXT_COLOR, Bundle.CV_PDF, MyCvPDFDefaultSettings.TEXT_COLOR),
				L10nUtil.getCVPDFLabel(Locales.CV_PDF, MyCvPDFLabelCodes.PAGE_NUMBER, "", pageNumber, totalPages),
				Settings.getFloat(MyCvPDFSettingCodes.PAGE_LEFT_MARGIN, Bundle.CV_PDF, MyCvPDFDefaultSettings.PAGE_LEFT_MARGIN)
						+ (pageWidth - Settings.getFloat(MyCvPDFSettingCodes.PAGE_LEFT_MARGIN, Bundle.CV_PDF, MyCvPDFDefaultSettings.PAGE_LEFT_MARGIN) - Settings.getFloat(
								MyCvPDFSettingCodes.PAGE_RIGHT_MARGIN, Bundle.CV_PDF, MyCvPDFDefaultSettings.PAGE_RIGHT_MARGIN)),
				Settings.getFloat(MyCvPDFSettingCodes.PAGE_LOWER_MARGIN, Bundle.CV_PDF, MyCvPDFDefaultSettings.PAGE_LOWER_MARGIN), PDFUtil.Alignment.BOTTOM_RIGHT);
		writer.closeContentStream();
	}

	@Override
	public PDRectangle getDefaultPageSize() {
		return DEFAULT_PAGE_SIZE;
	}

	public PDFont getFontA() {
		return fontA;
	}

	public PDFont getFontB() {
		return fontB;
	}

	public PDFont getFontC() {
		return fontC;
	}

	@Override
	public PageOrientation getPageOrientation() {
		if (Settings.getBoolean(MyCvPDFSettingCodes.LANDSCAPE, Bundle.CV_PDF, MyCvPDFDefaultSettings.LANDSCAPE)) {
			return PageOrientation.LANDSCAPE;
		} else {
			return PageOrientation.PORTRAIT;
		}
	}

	public CvPDFVO getPdfVO() {
		return pdfVO;
	}

	@Override
	public String getTemplateFileName() throws Exception {
		String key = L10nUtil.getDepartmentL10nKey(MyCvPDFSettingCodes.TEMPLATE_FILE_NAME, cursor.getStaff());
		if (Settings.containsKey(key, Bundle.CV_PDF)) {
			return Settings.getPDFTemplateFilename(key, Bundle.CV_PDF, null);
		}
		return Settings.getPDFTemplateFilename(MyCvPDFSettingCodes.TEMPLATE_FILE_NAME, Bundle.CV_PDF, null);
	}

	@Override
	public boolean hasNextBlock() {
		return blockIndex < blocks.size();
	}

	@Override
	public void loadFonts(PDDocument doc) throws Exception {
		fontA = PDFUtil.loadFont(Settings.getPDFFontName(MyCvPDFSettingCodes.FONT_A, Bundle.CV_PDF, null), doc, DEFAULT_BASE_FONT);
		fontB = PDFUtil.loadFont(Settings.getPDFFontName(MyCvPDFSettingCodes.FONT_B, Bundle.CV_PDF, null), doc, DEFAULT_BASE_FONT);
		fontC = PDFUtil.loadFont(Settings.getPDFFontName(MyCvPDFSettingCodes.FONT_C, Bundle.CV_PDF, null), doc, DEFAULT_BASE_FONT);
	}

	@Override
	public void loadImages(PDDocument doc) {
		if (imageVOMap != null && Settings.getBoolean(MyCvPDFSettingCodes.SHOW_IMAGES, Bundle.CV_PDF, MyCvPDFDefaultSettings.SHOW_IMAGES)) {
			Float width = Settings.getFloatNullable(MyCvPDFSettingCodes.IMAGE_WIDTH, Bundle.CV_PDF, MyCvPDFDefaultSettings.IMAGE_WIDTH);
			Float height = Settings.getFloatNullable(MyCvPDFSettingCodes.IMAGE_HEIGHT, Bundle.CV_PDF, MyCvPDFDefaultSettings.IMAGE_HEIGHT);
			int quality = Settings.getInt(MyCvPDFSettingCodes.IMAGE_QUALITY, Bundle.CV_PDF, MyCvPDFDefaultSettings.IMAGE_QUALITY);
			int dpi = Settings.getInt(MyCvPDFSettingCodes.IMAGE_DPI, Bundle.CV_PDF, MyCvPDFDefaultSettings.IMAGE_DPI);
			Iterator<StaffImageOutVO> it = imageVOMap.values().iterator();
			while (it.hasNext()) {
				StaffImageOutVO staffImage = it.next();
				if (staffImage.getHasImage() && staffImage.getShowCv()) {
					images.put(staffImage.getId(),
							PDFJpeg.prepareScaledImage(doc, staffImage.getDatas(), width == null ? 0 : width, height == null ? 0 : height, quality, dpi, null));
				}
			}
		}
	}

	@Override
	public boolean nextBlockFitsOnPage() throws Exception {
		MyCvPDFBlock block = blocks.get(blockIndex);
		if (blockIndex > 0 && BlockType.HEAD.equals(block.getType())) {
			return false;
		} else {
			return (cursor.getBlockY() - block.getHeight(cursor)) > Settings.getFloat(MyCvPDFSettingCodes.BLOCKS_LOWER_MARGIN, Bundle.CV_PDF,
					MyCvPDFDefaultSettings.BLOCKS_LOWER_MARGIN);
		}
	}

	@Override
	public void populateBlocks() {
		blocks.clear();
		if (staffVOs != null) {
			Iterator<StaffOutVO> staffIt = staffVOs.iterator();
			while (staffIt.hasNext()) {
				StaffOutVO staffVO = staffIt.next();
				blocks.add(new MyCvPDFBlock(staffVO, MyCvPDFBlock.BlockType.HEAD));
				PDFJpeg ximage = images.get(staffVO.getId());
				if (Settings.getBoolean(MyCvPDFSettingCodes.IMAGE_CENTERED, Bundle.CV_PDF, MyCvPDFDefaultSettings.IMAGE_CENTERED)) {
					blocks.add(new MyCvPDFBlock(ximage));
					blocks.add(new MyCvPDFBlock(staffVO, MyCvPDFBlock.BlockType.FULL_NAME));
					blocks.add(new MyCvPDFBlock(staffVO, MyCvPDFBlock.BlockType.DATE_OF_BIRTH));
					blocks.add(new MyCvPDFBlock(staffVO, MyCvPDFBlock.BlockType.ACADEMIC_TITLE));
					if (addressVOMap != null) {
						blocks.add(new MyCvPDFBlock(staffVO, addressVOMap.get(staffVO.getId())));
					}
				} else {
					if (addressVOMap != null) { // && ximage != null) {
						blocks.add(new MyCvPDFBlock(staffVO, addressVOMap.get(staffVO.getId()), ximage));
					}
				}
				if (allCvSectionVOs != null) {
					Iterator<CvSectionVO> sectionIt = allCvSectionVOs.iterator();
					boolean sectionsAppended = false;
					if (cvPositionVOMap != null) {
						HashMap<Long, Collection<CvPositionPDFVO>> staffPositionVOMap = cvPositionVOMap.get(staffVO.getId());
						if (staffPositionVOMap != null) {
							while (sectionIt.hasNext()) {
								CvSectionVO sectionVO = sectionIt.next();
								Collection<CvPositionPDFVO> cvPositions = staffPositionVOMap.get(sectionVO.getId());
								if (cvPositions != null && cvPositions.size() > 0) {
									blocks.add(new MyCvPDFBlock(sectionVO, cvPositions));
								} else if (sectionVO.getVisible()) {
									blocks.add(new MyCvPDFBlock(sectionVO, new ArrayList<CvPositionPDFVO>()));
								}
							}
							sectionsAppended = true;
						}
					}
					if (!sectionsAppended) {
						while (sectionIt.hasNext()) {
							CvSectionVO sectionVO = sectionIt.next();
							if (sectionVO.getVisible()) {
								blocks.add(new MyCvPDFBlock(sectionVO, new ArrayList<CvPositionPDFVO>()));
							}
						}
					}
				}
				blocks.add(new MyCvPDFBlock(staffVO, now));
			}
		}
	}

	@Override
	public void reset() {
		super.reset();
		blockIndex = 0;
		pageWidth = DEFAULT_PAGE_SIZE.getWidth();
		pageHeight = DEFAULT_PAGE_SIZE.getHeight();
		cursor.setBlockY(pageHeight - Settings.getFloat(MyCvPDFSettingCodes.BLOCKS_UPPER_MARGIN, Bundle.CV_PDF, MyCvPDFDefaultSettings.BLOCKS_UPPER_MARGIN));
		cursor.setBlockX(Settings.getFloat(MyCvPDFSettingCodes.BLOCKS_LEFT_MARGIN, Bundle.CV_PDF, MyCvPDFDefaultSettings.BLOCKS_LEFT_MARGIN));
		cursor.setBlockWidth(pageWidth - Settings.getFloat(MyCvPDFSettingCodes.BLOCKS_RIGHT_MARGIN, Bundle.CV_PDF, MyCvPDFDefaultSettings.BLOCKS_RIGHT_MARGIN)
				- Settings.getFloat(MyCvPDFSettingCodes.BLOCKS_LEFT_MARGIN, Bundle.CV_PDF, MyCvPDFDefaultSettings.BLOCKS_LEFT_MARGIN));
		cursor.setStaff(null);
		fontA = null;
		fontB = null;
		fontC = null;
		images.clear();
		updateCvPDFVO();
	}

	@Override
	public boolean save(ByteArrayOutputStream pdfStream) throws Exception {
		byte[] documentData = pdfStream.toByteArray();
		pdfVO.setMd5(CommonUtil.getHex(MessageDigest.getInstance("MD5").digest(documentData)));
		pdfVO.setSize(documentData.length);
		pdfVO.setDocumentDatas(documentData);
		return true;
	}

	public void setAddressVOMap(HashMap<Long, StaffAddressOutVO> addressVOMap) {
		this.addressVOMap = addressVOMap;
	}

	public void setAllCvSectionVOs(Collection<CvSectionVO> allCvSectionVOs) {
		this.allCvSectionVOs = allCvSectionVOs;
	}

	public void setCvPositionVOMap(HashMap<Long, HashMap<Long, Collection<CvPositionPDFVO>>> cvPositionVOMap) {
		this.cvPositionVOMap = cvPositionVOMap;
	}

	public void setImageVOMap(HashMap<Long, StaffImageOutVO> imageVOMap) {
		this.imageVOMap = imageVOMap;
	}

	@Override
	public void setPageHeight(float pageHeight) {
		this.pageHeight = pageHeight;
		cursor.setBlockY(pageHeight - Settings.getFloat(MyCvPDFSettingCodes.BLOCKS_UPPER_MARGIN, Bundle.CV_PDF, MyCvPDFDefaultSettings.BLOCKS_UPPER_MARGIN));
	}

	@Override
	public void setPageWidth(float pageWidth) {
		this.pageWidth = pageWidth;
		cursor.setBlockWidth(pageWidth - Settings.getFloat(MyCvPDFSettingCodes.BLOCKS_RIGHT_MARGIN, Bundle.CV_PDF, MyCvPDFDefaultSettings.BLOCKS_RIGHT_MARGIN)
				- Settings.getFloat(MyCvPDFSettingCodes.BLOCKS_LEFT_MARGIN, Bundle.CV_PDF, MyCvPDFDefaultSettings.BLOCKS_LEFT_MARGIN));
	}

	public void setStaffVOs(Collection<StaffOutVO> staffVOs) {
		this.staffVOs = staffVOs;
	}

	@Override
	public void startNewPage() {
		super.startNewPage(!hasNextBlock() || BlockType.HEAD.equals(blocks.get(blockIndex).getType()));
		cursor.setBlockY(pageHeight - Settings.getFloat(MyCvPDFSettingCodes.BLOCKS_UPPER_MARGIN, Bundle.CV_PDF, MyCvPDFDefaultSettings.BLOCKS_UPPER_MARGIN));
		cursor.setBlockX(Settings.getFloat(MyCvPDFSettingCodes.BLOCKS_LEFT_MARGIN, Bundle.CV_PDF, MyCvPDFDefaultSettings.BLOCKS_LEFT_MARGIN));
		cursor.setBlockWidth(pageWidth - Settings.getFloat(MyCvPDFSettingCodes.BLOCKS_RIGHT_MARGIN, Bundle.CV_PDF, MyCvPDFDefaultSettings.BLOCKS_RIGHT_MARGIN)
				- Settings.getFloat(MyCvPDFSettingCodes.BLOCKS_LEFT_MARGIN, Bundle.CV_PDF, MyCvPDFDefaultSettings.BLOCKS_LEFT_MARGIN));
	}

	@Override
	public void updateCursor() {
		MyCvPDFBlock block = blocks.get(blockIndex);
		if (BlockType.HEAD.equals(block.getType())) {
			cursor.setStaff(block.getStaff());
		}
	}

	private void updateCvPDFVO() {
		pdfVO.setContentTimestamp(now);
		pdfVO.setContentType(CoreUtil.getPDFMimeType());
		pdfVO.setStafves(staffVOs);
		StringBuilder fileName = new StringBuilder(CV_PDF_FILENAME_PREFIX);
		if (staffVOs != null && staffVOs.size() == 1) {
			fileName.append(staffVOs.iterator().next().getId());
			fileName.append("_");
		}
		fileName.append(CommonUtil.formatDate(now, CommonUtil.DIGITS_ONLY_DATETIME_PATTERN));
		fileName.append(".");
		fileName.append(CoreUtil.PDF_FILENAME_EXTENSION);
		pdfVO.setFileName(fileName.toString());
	}
}
