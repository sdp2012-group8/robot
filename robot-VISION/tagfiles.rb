#!/usr/bin/ruby

require_relative "gui"

localfolder = File.expand_path(File.dirname(__FILE__))
imagefolder = "data/testImages/"
File.open('./xml/imagedata.xml','w') do |f|
  f.puts "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
  f.puts "<annotations>"
  Dir.entries("#{localfolder}/#{imagefolder}").each do |image|
    next if (!image.include? ".png")
    f.puts "<image>"
    f.puts "<filename>"
    f.puts "#{imagefolder}#{image}"
    f.puts "</filename>"
    imagelink = "#{localfolder}/#{imagefolder}#{image}"
    puts "Loading #{imagelink}"
    window = GameWindow.new("#{imagelink}")
    window.show
    f.puts "<location-data>"
    f.puts "<ball>"
    f.puts "<x>"
    f.puts window.ballpos[0]
    f.puts "</x>"
    f.puts "<y>"
    f.puts window.ballpos[1]
    f.puts "</y>"
    f.puts "</ball>"
    f.puts "<bluerobot>"
    f.puts "<x>"
    f.puts window.robotpos("blue")[0]
    f.puts "</x>"
    f.puts "<y>"
    f.puts window.robotpos("blue")[1]
    f.puts "</y>"
    f.puts "<angle>"
    f.puts window.robotangle("blue")
    f.puts "</angle>"
    f.puts "</bluerobot>"
    f.puts "<yellowrobot>"
    f.puts "<x>"
    f.puts window.robotpos("yellow")[0]
    f.puts "</x>"
    f.puts "<y>"
    f.puts window.robotpos("yellow")[1]
    f.puts "</y>"
    f.puts "<angle>"
    f.puts window.robotangle("yellow")
    f.puts "</angle>"
    f.puts "</yellowrobot>"
    f.puts "</location-data>"
    f.puts "</image>"
  end
f.puts "</annotations>"
end
