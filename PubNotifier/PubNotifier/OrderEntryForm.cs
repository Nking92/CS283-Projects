using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.IO;
using System.Linq;
using System.Net.Sockets;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace PubNotifier
{
    public partial class OrderEntryForm : Form
    {
        private List<int> _orderNums = new List<int>();
        private const int MAX_NUM_ORDERS = 75;
        private const int MAX_ORDER_VALUE = 10000;

        public OrderEntryForm()
        {
            InitializeComponent();
        }

        private void postOrderButton_Click(object sender, EventArgs e)
        {
            SubmitText();
        }

        private void OrderEntryForm_Load(object sender, EventArgs e)
        {
            orderListBox.DataSource = _orderNums;
        }

        private void SubmitText()
        {
            int num;
            try
            {
                num = Convert.ToInt32(orderTextBox.Text);
                if (num > MAX_ORDER_VALUE)
                {
                    MessageBox.Show("Enter a number smaller than " + MAX_ORDER_VALUE, "Invalid input", MessageBoxButtons.OK, MessageBoxIcon.Exclamation);
                }
                else if (num < 0)
                {
                    MessageBox.Show("Enter a nonnegative number", "Invalid input", MessageBoxButtons.OK, MessageBoxIcon.Exclamation);
                }
                else
                {
                    _orderNums.Insert(0, num);
                    if (_orderNums.Count > MAX_NUM_ORDERS)
                    {
                        _orderNums.RemoveAt(MAX_NUM_ORDERS);
                    }
                    orderTextBox.Clear();
                    // Force the listbox to refresh its content
                    orderListBox.DataSource = null;
                    orderListBox.DataSource = _orderNums;
                    Worker worker = new Worker(num);
                    Thread workerThread = new Thread(worker.DoWork);

                    // Start the worker thread.
                    workerThread.Start(); 
                }
            }
            catch (FormatException)
            {
                MessageBox.Show("Please enter a number", "Invalid input", MessageBoxButtons.OK, MessageBoxIcon.Exclamation);
            }
        }

    }

    public class Worker
    {
        private const string SERVER_IP = "54.186.61.49";
        private const int SERVER_PORT = 20050;
        private int _n;

        public Worker(int n)
        {
            _n = n;
        }

        public void DoWork()
        {
            try
            {
                TcpClient client = new TcpClient(SERVER_IP, SERVER_PORT);
                StreamWriter writer = new StreamWriter(client.GetStream());
                writer.Write("post " + _n);
                writer.Flush();
                writer.Close();
                client.Close();
            }
            catch (SocketException)
            {
                // Log the error
            }
            catch (IOException)
            {
                // Log the error
            }
        }
    }

}
