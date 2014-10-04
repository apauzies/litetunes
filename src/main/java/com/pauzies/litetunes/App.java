package com.pauzies.litetunes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/*import java.nio.file.DirectoryStream;
 import java.nio.file.FileSystems;
 import java.nio.file.Files;
 import java.nio.file.Path;*/

/**
 * Hello world!
 * 
 */
public class App {

	static Display display;
	static ToolItem playButton;
	static volatile Player player;
	volatile static boolean isPlaying;
	static Path playingSong;
	volatile static int playPosition;
	static Thread playerThread;
	static List<Path> files = new ArrayList<Path>();
	static TableViewer tv;
	static SongFilter tvFilter = new SongFilter();
	static Label statusBar;
	
	public static class Path {
		// Do not save the full path... (memory usage...)
		private final File path;

		public Path(File path) {
			this.path = path;
		}

		public File toFile() {
			return path;
		}
	}

	static class Mp3LabelProvider extends LabelProvider implements ITableLabelProvider {

		public Image getColumnImage(Object arg0, int arg1) {
			// TODO Auto-generated method stub
			return null;
		}

		public String getColumnText(Object arg0, int arg1) {
			Object[] entry = (Object[]) arg0;
			Integer i = (Integer) entry[0];
			Path path = (Path) entry[1];
			/*
			 * System.err.println(path); // String name =
			 * path.toFile().getName(); try { Mp3File mp3 = new
			 * Mp3File(path.toString()); ID3v2 id3 = mp3.getId3v2Tag(); if (id3
			 * != null && id3.getArtist() != null && id3.getTitle() != null) {
			 * return id3.getArtist() + " - " + id3.getTitle(); } ID3v1 id3v1 =
			 * mp3.getId3v1Tag(); if (id3v1 != null && id3v1.getArtist() != null
			 * && id3v1.getTitle() != null) { return id3v1.getArtist() + " - " +
			 * id3v1.getTitle(); } return path.toFile().getName(); } catch
			 * (UnsupportedTagException e) { // TODO Auto-generated catch block
			 * // e.printStackTrace(); } catch (InvalidDataException e) { //
			 * TODO Auto-generated catch block // e.printStackTrace(); } catch
			 * (IOException e) { // TODO Auto-generated catch block //
			 * e.printStackTrace(); }
			 */
			return i + " - " + path.toFile().getName();
		}

	}

	static class Mp3ContentProvider implements IStructuredContentProvider {

		public Object[] getElements(Object inputElement) {
			Object[] objs = new Object[files.size()];
			for (int i = 0; i < files.size(); i++) {
				Object[] entry = new Object[2];
				entry[0] = i;
				entry[1] = files.get(i);
				objs[i] = entry;
			}
			return objs;
		}

		public void dispose() {
			// TODO Auto-generated method stub

		}

		public void inputChanged(Viewer arg0, Object arg1, Object arg2) {
			arg0.refresh();
		}

	}

	static void createColumns(TableViewer viewer) {
		TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
		column.getColumn().setText("name");
		// column.getColumn().set
		column.getColumn().setWidth(650);

		// column = new TableViewerColumn(viewer, SWT.NONE);
		// column.getColumn().setText("artist");
		// column.getColumn().setWidth(200);

		// column = new TableViewerColumn(viewer, SWT.NONE);
		// column.getColumn().setText("album");
		// column.getColumn().setWidth(200);
	}

	static void createViewer(Composite parent) {

		tv = new TableViewer(parent, SWT.VIRTUAL);
		tv.getTable().setHeaderVisible(false);
		tv.getTable().setLinesVisible(false);
		// tv.getTable().setBackgroundMode(SWT.NO_BACKGROUND);
		tv.getTable().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		tv.getTable().setForeground(new Color(Display.getDefault(), 133, 133, 133));
		createColumns(tv);

		tv.setContentProvider(new Mp3ContentProvider());
		tv.setLabelProvider(new Mp3LabelProvider());
		tv.addFilter(tvFilter);
		// tv.setInput(getServers());

		tv.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent arg0) {
				IStructuredSelection selection = (IStructuredSelection) arg0.getSelection();
				Object[] entry = (Object[]) selection.getFirstElement();
				playPosition = (Integer) entry[0];
				// Path path = (Path) entry[1];
				playFile(playPosition);
			}
		});

		tv.getTable().pack();
	}



	static void playFile(int songNumber) {
		final Path path = files.get(songNumber);
		playPosition = songNumber;

		try {
			if (player != null) {
				player.close();
				playerThread.stop();
			}

			player = new Player(new FileInputStream(path.toFile()));
			// player = new Player(new FileInputStream(path.toFile()));
			playingSong = path;
			playerThread = new Thread(new Runnable() {

				public void run() {
					// TODO Auto-generated method stub
					try {
						isPlaying = true;
						Display.getDefault().syncExec(new Runnable() {

							public void run() {
								statusBar.setText("> " + path.toFile().getName());
								// statusBar.pack();
								playButton.setText("pause");
							}
						});

						player.play();

						while (playPosition < files.size() - 1) {
							++playPosition;
							player.close();
							final Path path2 = files.get(playPosition);
							try {
								player = new Player(new FileInputStream(path2.toFile()));
								player.play();
								Display.getDefault().syncExec(new Runnable() {

									public void run() {
										statusBar.setText("> " + path2.toFile().getName());
										// statusBar.pack();
										playButton.setText("pause");
									}
								});
							} catch (FileNotFoundException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							player.play();
						}
						// player.play();
						player.close();
						isPlaying = false;
					} catch (JavaLayerException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});
			playerThread.start();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JavaLayerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	static void addAllFiles(List<Path> result, final String directoryPath) {
		File dir = new File(directoryPath);
		for (File f : dir.listFiles()) {
			if (f.isDirectory()) {
				try {
					Display.getDefault().syncExec(new Runnable() {
						public void run() {
							statusBar.setText(String.format("Scanning %s", directoryPath));
							tv.refresh();
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				}
				addAllFiles(result, f.getPath());
				// tv.refresh();
			} else if (f.getName().endsWith(".mp3")) {
				result.add(new Path(f));
			}
		}
	}

	// static void addAllFiles2(List<Path> result, String directoryPath) {
	// // statusBar.setText(String.format("scanning %s",directoryPath));
	// // statusBar.pack();
	// try (DirectoryStream<Path> ds =
	// Files.newDirectoryStream(FileSystems.getDefault().getPath(directoryPath)))
	// {
	// for (Path p : ds) {
	// if (p.toFile().isDirectory()) {
	// addAllFiles(result, p.toString());
	// } else {
	// if (p.toString().endsWith(".mp3")) {
	// result.add(p);
	// }
	// }
	// }
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// }

	private static void addFiles(Composite parent) {
		DirectoryDialog fd = new DirectoryDialog(parent.getShell(), SWT.OPEN);
		fd.setText("Open");
		// fd.setFilterPath(System.getProperty("user.home"));
		// String[] filterExt = { "*.*" };
		// fd.setFilterExtensions(filterExt);
		final String selected = fd.open();
		if (selected == null) {
			return;
		}
		new Thread(new Runnable() {
			public void run() {
				addAllFiles(files, selected);
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						if (!isPlaying) {
							statusBar.setText(String.format("Done scanning %s!", selected));
						} else {
							statusBar.setText(String.format("> %s", playingSong.toFile().getName()));
						}
						tv.refresh();
					}
				});
			}
		}).start();

		// for (Path p : files) {
		// System.err.println(p);
		// }
		tv.setInput(files.toArray());
	}

	static class SongFilter extends ViewerFilter {
		static String searchString;

		static void setSearchText(String s) {
			// Search must be a substring of the existing value
			searchString = ".*" + s + ".*";
		}

		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (searchString == null || searchString.length() == 0) {
				return true;
			}
			Object[] entry = (Object[]) element;
			Path p = (Path) entry[1];
			if (p.toFile().getName().toLowerCase().matches(searchString)) {
				return true;
			}

			return false;
		}
	}

	private static Composite createSearchBar(Composite parent) {
		Composite searchBar = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.verticalSpacing = 1;
		layout.marginHeight = 1;
		layout.horizontalSpacing = 0;
		layout.marginWidth = 1;
		layout.marginRight = 3;
		layout.marginLeft = 3;
		layout.marginTop = 3;
		searchBar.setLayout(layout);
		searchBar.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
		searchBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Composite c1 = new Composite(searchBar, SWT.NONE);
		layout = new GridLayout();
		layout.verticalSpacing = 1;
		layout.marginHeight = 1;
		layout.horizontalSpacing = 0;
		layout.marginWidth = 1;
		// layout.marginRight = 10;
		c1.setLayout(layout);
		c1.setBackground(new Color(display, 56, 56, 56));
		c1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		final Text searchText = new Text(c1, SWT.NONE);
		searchText.setText("search...");
		searchText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		searchText.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
		// searchBar.setBackground(new Color(display, 56,56,56));
		// searchBar.setForeground(display.getSystemColor(SWT.COLOR_GREEN));
		searchText.setForeground(new Color(Display.getDefault(), 133, 133, 133));
		searchText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				tvFilter.setSearchText(searchText.getText());
				tv.refresh();
			}
		});
		searchBar.addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				if (searchText.getText().equals("search...")) {
					searchText.setText("");
				}
			}
			public void focusLost(FocusEvent e) {
				if (searchText.getText().equals("")) {
					// searchBar.setText("search...");
					// tvFilter.setSearchText("");
				}
			}
		});
		searchBar.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.CR) {
					tv.getTable().setFocus();
				}
			}
		});
		return searchBar;
	}
	
	private static Composite createToolbar(Composite parent) {
		final ToolBar bar = new ToolBar(parent, SWT.NONE);
		bar.setLayout(new RowLayout());
		GridData layoutData1 = new GridData();
		layoutData1.grabExcessHorizontalSpace = true;
		layoutData1.horizontalAlignment = SWT.FILL;
		// layoutData1.minimumWidth = 500;
		bar.setLayoutData(layoutData1);
		bar.setForeground(display.getSystemColor(SWT.COLOR_GREEN));

		ToolItem openButton = new ToolItem(bar, SWT.PUSH);
		openButton.setText("[ + ]");
		openButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				addFiles(bar);
			}
		});

		ToolItem previousButton = new ToolItem(bar, SWT.PUSH);
		previousButton.setText("[<<");
		previousButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (playPosition > 0) {
					playFile(--playPosition);
				}
				// addFiles(bar);
			}
		});

		playButton = new ToolItem(bar, SWT.PUSH);
		playButton.setText("");
		playButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// addFiles(bar);
				if (isPlaying) {
					isPlaying = false;
					playButton.setText("resume");
					playerThread.suspend();
				} else {
					playerThread.resume();
					isPlaying = true;
					playButton.setText("pause");
				}
			}
		});

		ToolItem nextButton = new ToolItem(bar, SWT.PUSH);
		nextButton.setText(">>]");
		nextButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				if (playPosition < files.size() - 1) {
					playFile(++playPosition);
				}
			}

		});

		return bar;
	}
	
	private static Label createStatusBar(Composite parent) {
		// Composite c = new Composite(shell, SWT.NONE);
		statusBar = new Label(parent, SWT.NONE);
		statusBar.setText("--");
		statusBar.setForeground(display.getSystemColor(SWT.COLOR_CYAN));
		statusBar.setBackground(new Color(display, 56, 56, 56));
		GridData layoutData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		layoutData.horizontalSpan = 2;
		statusBar.setLayoutData(layoutData);
		return statusBar;
	}
	
	private static void createMain(Composite parent) {
		GridData layoutData = new GridData();
		layoutData.grabExcessHorizontalSpace = true;
		layoutData.grabExcessVerticalSpace = true;
		layoutData.horizontalAlignment = GridData.FILL;
		layoutData.verticalAlignment = GridData.FILL;
		layoutData.horizontalSpan = 2;
		createViewer(parent);
		tv.getTable().setLayoutData(layoutData);
		// compo.pack();
	}
	public static void main(String[] args) {
		display = new Display();
		Shell shell = new Shell(display);
		shell.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
		shell.setText("liteTunes");
		// shell.setBackgroundMode(SWT.INHERIT_DEFAULT);

		GridLayout l = new GridLayout(2, true);
		l.marginHeight = 0;
		l.marginWidth = 0;
		l.horizontalSpacing = 0;
		l.verticalSpacing = 0;
		l.marginRight = 0;
		l.marginLeft = 0;
		l.marginTop = 0;
		l.marginBottom = 0;
		shell.setLayout(l);

		createToolbar(shell);
		createSearchBar(shell);
		createMain(shell);
		createStatusBar(shell);

		shell.pack();
		shell.setSize(700, 350);
		shell.open();

		tv.getTable().setFocus();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		display.dispose();
	}
}
