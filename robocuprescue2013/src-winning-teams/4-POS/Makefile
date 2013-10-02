#
#  Copyright 2002-2009, Mersad Team, Allameh Helli High School (NODET).
#
#  This program is free software, you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation.
#
#  This program is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
#  GNU Library General Public License for more details.
#
#  This file is created by: Mohsen Izadi
#  	and is modified by: Ahmad Boorghany, Sassan Haradji, Pooria Kaviani
#
#  Released on Friday 25 September 2009, 3 Mehr 1388 by Mersad RoboCup Team.
#  For more information please read README file.
#

TOPDIR = .

PWD = $(shell pwd)
BASEPWD = $(shell basename $(shell pwd))
AGENTVERSION = ${BASEPWD}
VERNAME = ${BASEPWD}

OWNER = Final
ARCHIVEDIR = ../Archive

clean:
	rm -rf bin
	rm -rf LogFiles/*
	rm -rf jars
	rm -rf test-results

bz2:
	rm -rf /tmp/$(BASEPWD)
	cp -r $(PWD) /tmp/$(BASEPWD)
	cp -r /tmp/$(BASEPWD) $(PWD)
	rm -rf /tmp/$(BASEPWD)
	make -C ./$(BASEPWD) clean
	-tar -jcf ../$(VERNAME).tar.bz2 $(BASEPWD)/*
	rm -rf /tmp/$(BASEPWD)
	rm -rf $(BASEPWD)

bz2time:
	rm -rf /tmp/$(BASEPWD)
	cp -r $(PWD) /tmp/$(BASEPWD)
	cp -r /tmp/$(BASEPWD) $(PWD)
	rm -rf /tmp/$(BASEPWD)
	make -C ./$(BASEPWD) clean
	@date "+$(AGENTVERSION)%nDate: %Y/%m/%d%nTime: %H:%M:%S%n" > ./$(BASEPWD)/ARCHIVE
	-tar -jcf ../$(VERNAME)-`date +%Y_%m_%d-%H_%M`.tar.bz2 $(BASEPWD)/*
	rm -rf /tmp/$(BASEPWD)
	rm -rf $(BASEPWD)
