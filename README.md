# WARC Extractor

If you have WARC files with HTML and need to extract text from there, this is 
the project that you need. You typically use it to  
extract text information from [Common Crawl](http://commoncrawl.org).


## Common Crawl

Common Crawl (http://commoncrawl.org) is a copy of the Internet. Each month they release all the 
websites that they crawled in the previous month. This is a lot of data (many terabytes) and a 
good source dataset for many NLP tasks.

In many cases you probaby don't need to use all the data. Is it not difficult to get just 
a few files. 

Here is how:

- Go to http://commoncrawl.org/the-data/get-started/ to the "Data Location" section
- Copy the url for the data you're interested in
  - E.g. `s3://commoncrawl/crawl-data/CC-MAIN-2018-09` (for February 2018)
- Replace `s3://commoncrawl/crawl-data/` with `https://commoncrawl.s3.amazonaws.com/`
- To get paths to all the files in the archive, you need to get the `warc.paths.gz` file
  - It is located in the root of each month's folder
  - So to get the paths for February 2018, use https://commoncrawl.s3.amazonaws.com/crawl-data/CC-MAIN-2018-05/warc.paths.gz
- Next sample 10 (or 100 or whatever) files from this list
- Append `https://commoncrawl.s3.amazonaws.com/` to each path, download 

In script:

    wget https://commoncrawl.s3.amazonaws.com/crawl-data/CC-MAIN-2018-09/warc.paths.gz

    zcat warc.paths.gz \
        | head -n 100 \
        | awk '{ print "https://commoncrawl.s3.amazonaws.com/" $0}' \
        > files.txt

    cat files.txt | parallel --gnu "wget {}"

Here we take top 100 files from this list and download them with GNU Parallel 


## Running WARC Extractor 

First you need to build it:

- `./build.sh`

You need to have java and maven to do it.

Next, you run it:

    java -cp libs/*:. warc.WarcPreparationJob \
        --input /path/to/input/warc/files \
        --output /output/path \
        --languages en,de

Parameters:

- `--input`: path to the downloaded WARC files
- `--output`: path where the results should be stored
- `--languages`: languages you want to keep, all others will be discarded 
