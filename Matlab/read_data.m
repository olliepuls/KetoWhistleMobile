clear all;
close all;
data = load("Acet2019-10-04T204924.21.txt");
% lim = 1319;
plot(data(:,1), data(:,2))